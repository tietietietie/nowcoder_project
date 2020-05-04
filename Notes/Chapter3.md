# 第三章：开发社区核心功能

## 过滤敏感词

### 前缀树数据结构定义

### 初始化前缀树

### 编写过滤敏感词方法

```java
@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    private static final String REPLACEMENT = "***";
    private TrieNode rootNode = new TrieNode();

    @PostConstruct
    public void init() {
        //类路径加载资源
        try (
                //获得缓冲字节流
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        ) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                //添加到前缀树
                this.addKeyWord(keyword);
            }

        } catch (IOException e) {
            logger.error("加载敏感词失败" + e.getMessage());
        }
    }

    private void addKeyWord(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode child = tempNode.getChild(c);
            if (child == null) {
                child = new TrieNode();
                tempNode.addChild(c, child);
            }
            tempNode = child;
        }
        tempNode.setKeywordEnd(true);
    }

    private class TrieNode {
        //关键词标识
        private boolean isKeywordEnd = false;
        //子节点
        private Map<Character, TrieNode> children = new HashMap<>();

        public boolean isKeywordEnd() {
            return isKeywordEnd;
        }

        public void setKeywordEnd(boolean keywordEnd) {
            isKeywordEnd = keywordEnd;
        }

        //添加子节点
        public void addChild(Character c, TrieNode node) {
            children.put(c, node);
        }

        //获取子节点
        public TrieNode getChild(Character c) {
            return children.get(c);
        }
    }

    //过滤敏感词
    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        TrieNode tempNode = rootNode;
        int len = text.length();
        StringBuilder ans = new StringBuilder();
        int start = 0, end = 0;
        while (end < len) {
            char c = text.charAt(end);
            if (isSymbol(c)) {
                if (tempNode == rootNode) {
                    ans.append(c);
                    start++;
                }
                end++;
                continue;
            }
            tempNode = tempNode.getChild(c);
            if (tempNode == null) {
                ans.append(text.charAt(start));
                start++;
                end = start;
                tempNode = rootNode;
            } else if (tempNode.isKeywordEnd()) {
                ans.append(REPLACEMENT);
                start = end + 1;
                end = start;
                tempNode = rootNode;
            } else {
                end++;
            }
        }

        ans.append(text.substring(start));
        return ans.toString();
    }

    //判断是否为符号
    private boolean isSymbol(Character c) {
        return !CharUtils.isAsciiAlphanumeric(c) && (c < 0x2E80 || c > 0x9FFF);
    }
}
```

## 发布帖子

异步请求：当前网页不刷新，访问服务器，服务器返回一些结果（不是网页），通过这个结果，给网页局部的刷新。

实现技术：AJAX：异步的JavaScript和XML，不是新技术。目前一般不适用XML，而是使用JSON，便于解析。

功能：网页能够增量更新呈现在网页上，而不是刷新整个页面。

手册：Mozilla/AJAX

### jQuery发送AJAX的示例

处理JSON字符串：引入fastjson

写一个简单的静态页面，使用JQuery来发送数据，并接收来自服务器的JSON数据

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>ajax</title>
</head>
<body>
<p>
    <input type="button" value="发送" onclick="send();">
</p>

<script src="https://code.jquery.com/jquery-3.3.1.min.js" crossorigin="anonymous"></script>
<script>
       function send(){
            $.post(
                "/community/alpha/ajax",
                {"name":"张三","age":23},
                function(data){
                    console.log(typeof(data));
                    console.log(data);

                    data = $.parseJSON(data);
                    console.log(typeof(data));
                    console.log(data.code);
                    console.log(data.msg);
                }
            );
       }
</script>
```

服务端控制层代码如下：

```java
@RequestMapping(path = "/ajax", method = RequestMethod.POST)
@ResponseBody
public String testAjax(String name, int age) {
    System.out.println(name);
    System.out.println(age);
    return CommunityUtil.getJSONString(0, "操作成功");
}
```

转换成JSON的小工具：

```java
//整合发送给浏览器的json数据
public static String getJSONString(int code, String msg, Map<String, Object> map) {
    JSONObject json = new JSONObject();
    json.put("code", code);
    json.put("msg", msg);
    if (map != null) {
        for (String key : map.keySet()) {
            json.put(key, map.get(key));
        }
    }
    return json.toJSONString();
}
```

### 利用发布帖子功能

还是按照数据访问层--->服务层--->控制层的顺序编写代码,

#### 数据访问层

在dao中的接口定义插入函数,然后再相应的mapper.xml中实现,将帖子插入数据库

```xml
<sql id="insertFields">
    user_id, title, content, type, status, create_time, comment_count, score
</sql>
<insert id="insertDiscussPost" parameterType="DiscussPost">
    insert into discuss_post(<include refid="insertFields"></include>)
    values(#{userId},#{title},#{content},#{type},#{status},#{createTime},#{commentCount},#{score})
</insert>
```

#### 业务层

将帖子插入数据库,过滤敏感词,转义html标签

```java
public int addDiscussPost(DiscussPost post) {
    if (post == null) {
        throw new IllegalArgumentException("参数不能为空");
    }
    //转义HTML标记
    post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
    post.setContent(HtmlUtils.htmlEscape(post.getContent()));
    //过滤敏感词
    post.setTitle(sensitiveFilter.filter(post.getTitle()));
    post.setContent(sensitiveFilter.filter(post.getContent()));
    return discussPostMapper.insertDiscussPost(post);
}
```

#### 控制层

获得服务器传来的数据,返回JSON字符串

```java
@RequestMapping(path = "/add", method = RequestMethod.POST)
@ResponseBody
public String addDiscussPost(String title, String content) {
    User user = hostHolder.getUser();
    if (user == null) {
        return CommunityUtil.getJSONString(403, "你还没有登录");
    }
    DiscussPost post = new DiscussPost();
    post.setUserId(user.getId());
    post.setTitle(title);
    post.setContent(content);
    post.setCreateTime(new Date());
    discussPostService.addDiscussPost(post);
    //程序如果报错，将来会统一处理
    return CommunityUtil.getJSONString(0, "发布成功！");
}
```

更改Index文件,写js代码,处理异步消息

```javascript
$(function(){
	$("#publishBtn").click(publish);
});

function publish() {
	$("#publishModal").modal("hide");
	//先返回结果再显示
    //获取标题/内容
    var title =  $("#recipient-name").val();
    var content = $("#message-text").val();
    //发送异步请求
    $.post(
        "/community/discuss/add",
        {"title":title,"content":content},
        function(data){
            data = $.parseJSON(data);
            //提示框显示返回消息
            $("#hintBody").text(data.msg);
            //显示提示框/两秒后自动隐藏
            $("#hintModal").modal("show");
            setTimeout(function(){
                $("#hintModal").modal("hide");
                if(data.code == 0) {
                    window.location.reload();
                }
            }, 2000);
        }
    );
}
```

最终效果如下:

![image-20200501211623977](Chapter3.assets/image-20200501211623977.png)

## 显示帖子详情

利用之前所学的内容，就能实现：

1,数据访问层：查寻帖子

2，业务层：查看帖子

3，控制层：处理查询请求

Index.html：处理详情帖子的链接

discuss-detail.html：显示帖子

### 开发三层代码

数据访问层：

```xml
<select id="selectDiscussPostById" resultType="DiscussPost">
    select
    <include refid="selectFields"></include>
    from discuss_post
    where id = #{id}
</select>
```

用户层：

```java
public DiscussPost findDiscussPostById(int id) {
    return discussPostMapper.selectDiscussPostById(id);
}
```

控制层

```java
@RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model) {
    //查询帖子
    DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
    model.addAttribute("post", post);
    //需要显示用户信息，而不是id
    //两种办法，在mapper中关联查询，也可以在controller中查userService，获得用户信息,这样效率会低一点
    //但是之后可以用redies提高速度
    User user = userService.findUserById(post.getUserId());
    model.addAttribute("user", user);
    return "/site/discuss-detail";
}
```

### HTML模板代码

修改index.html的链接

```html
<a th:href="@{|/discuss/detail/${map.post.id}|}" th:utext="${map.post.title}">备战春招，面试刷题跟他复习，一个月全搞定！</a>
```

修改discuss-detail.html的内容

```html
<span th:utext="${post.title}">备战春招，面试刷题跟他复习，一个月全搞定！</span>
<img th:src="${user.headerUrl}"
<div class="mt-0 text-warning" th:utext="${user.username}">寒江雪</div>
<b th:text="${#dates.format(post.createTime,'yyyy-MM-dd HH:mm:ss')}">2019-04-15 15:32:18</b>
<div class="mt-4 mb-3 content" th:utext="${post.content}">
```

## 事务管理

### 概念回顾

什么是事务：N步数据库操作序列，要么全部执行，要么全部放弃执行（以业务为单元，判断此次操作是否有效）

特性：原子性/一致性/隔离性/持久性

隔离性：针对并发而言，每个线程执行的事务互不干扰。

如果在多线程环境下，没有做多线程隔离（每一个浏览器访问服务器，多会创建一个线程）（多个用户同时访问同一份事务）（需要隔离性处理）

常见并发异常：1，更新。2，脏读。

分层级解决并发异常：串行化：最高级别，加锁，能解决所有问题，但是会造成性能下降。

一般选择：读取已提交数据/可重复读 这两种级别

常见并发异常的含义：

1. 第一类丢失更新：某一个事务的回滚，导致了另外一个事务已更新的数据丢失了。
2. 第二类丢失更新：某一个事务的提交，导致另一个事务已更新的数据丢失了
3. 脏读：某一个事务读取了另一个事务未提交的数据，
4. 不可重复读：某一个事务，对同一数据前后读取结果不一致
5. 幻读：某一事务，对同一个表，查询到的行数不一致

越不安全效率越高。

事务隔离级别与并发异常表格：

实现机制：

悲观锁（数据库）包括共享锁和排他锁

乐观锁（自定义）：更新数据前，需要检查版本号是否变化，如果版本号变了，就取消本次更新

Spring事务管理：

Spring引以为豪的技术点，无论底层是什么数据（库），Spring对其事务管理API都是统一的，非常方便。

* 在XML或者注解，加上配置就行
* 也可以编程式事务，通过Transaction Template管理事务（自由度高，对某一步数据库操作进行管理）

### Spring事务管理示例

可以使用注解，代码如下：最终执行结果，数据库中不会出现新的用户和帖子

```java
//新增用户+自动发一个新人贴
//传播机制解释：两个事务交叉在一起，如何管理
//REQUIRED：支持当前事务（支持外部事务）
//REQUIRES_NEW
//NESTED:如果当前存在事务（外部事务），则嵌套在该事务中执行
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
public Object save1() {
    //新建用户
    User user = new User();
    user.setUsername("aplha");
    user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
    user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
    user.setEmail("Alpha@qq.com");
    user.setHeaderUrl("http://image/nowcoder.com/head/99t.png");
    user.setCreatTime(new Date());
    userMapper.insertUser(user);
    //新建帖子
    DiscussPost post = new DiscussPost();
    post.setUserId(user.getId());
    post.setTitle("新人贴");
    post.setContent("新人报到，多多指教");
    post.setCreateTime(new Date());
    discussPostMapper.insertDiscussPost(post);
    //报错前会把数据插入吗？
    Integer.valueOf("abc");
    return "ok";
}
```

也可以使用TransactionTemplate实现

```java
public Object save2() {
    transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    return transactionTemplate.execute(new TransactionCallback<Object>() {

        @Override
        public Object doInTransaction(TransactionStatus transactionStatus) {
            //新建用户
            User user = new User();
            user.setUsername("beta");
            user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
            user.setPassword(CommunityUtil.md5("123" + user.getSalt()));
            user.setEmail("beta@qq.com");
            user.setHeaderUrl("http://image/nowcoder.com/head/999.png");
            user.setCreatTime(new Date());
            userMapper.insertUser(user);
            //新建帖子
            DiscussPost post = new DiscussPost();
            post.setUserId(user.getId());
            post.setTitle("新人贴,我叫beta");
            post.setContent("新人报到，多多指教~");
            post.setCreateTime(new Date());
            discussPostMapper.insertDiscussPost(post);
            //报错前会把数据插入吗？
            Integer.valueOf("abc");
            return "ok";
        }
    });
}
```

## 显示评论

还是分成三层进行开发

### 数据层

查询一页的评论，需要明确数据表中每个字段的含义：
entity_type:表示评论的对象类型：可以评论帖子，可以评论题目等等，本项目评论对象为帖子

entity_id：表示帖子的Id

target_id：表示这条评论是指向哪个用户的。

```java
@Mapper
public interface CommentMapper {
    //查询某一页数据/一共多少条数据
    List<Comment> selectCommentByEntity(@Param("entityType") int entityType, @Param("entityId") int entityId, @Param("offset") int offset, @Param("limit") int limit);

    int selectCountByEntity(@Param("entityType") int entityType, @Param("entityId") int entityId);
}
```

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.nowcoder.community.dao.CommentMapper">
    <sql id="selectFields">
        id, user_id, entity_type, entity_id, target_id, content, status, create_time
    </sql>

    <select id="selectCommentByEntity" resultType="Comment">
        select
        <include refid="selectFields"></include>
        from comment
        where status = 0
        and entity_type = #{entityType}
        and entity_id = #{entityId}
        order by create_time asc
        limit #{offset}, #{limit}
    </select>

    <select id="selectCountByEntity" resultType="int">
        select count(id)
        from comment
        where status = 0
        and entity_type = #{entityType}
        and entity_id = #{entityId}
    </select>
</mapper>
```

### 服务层

根据评论类型，评论对象，以及页码的一些数据，可以找到所需评论

```java
@Service
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;

    //查询某一页数据
    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentCount(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }
}
```

### 控制层

需要将查找到的评论装入一个commentVoList，然后装入Model

```java
    //SpringMVC每次会把参数中的bean(Page)都自动放在Model里面
    @RequestMapping(path = "/detail/{discussPostId}", method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model, Page page) {
        //查询帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);
        //需要显示用户信息，而不是id
        //两种办法，在mapper中关联查询，也可以在controller中查userService，获得用户信息,这样效率会低一点
        //但是之后可以用redies提高速度
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);
        //评论分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        page.setRows(post.getCommentCount());
        //评论：帖子的评论
        //回复：评论的评论
        //评论的列表
        List<Comment> commentList =
                commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //评论的VO列表
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                //一个评论的VO
                Map<String, Object> commentVo = new HashMap<>();
                //往VO添加评论
                commentVo.put("comment", comment);
                //往VO添加作者
                commentVo.put("user", userService.findUserById(comment.getUserId()));
                //添加回复
                List<Comment> replyList =
                        commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                //回复的VO列表
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        Map<String, Object> replyVo = new HashMap<>();
                        replyVo.put("reply", reply);
                        replyVo.put("user", userService.findUserById(reply.getUserId()));
                        //回复目标
                        if (reply.getTargetId() != 0) {
                            replyVo.put("target", userService.findUserById(reply.getTargetId()));
                        } else
                            replyVo.put("target", null);
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys", replyVoList);
                commentVo.put("replyCount", commentService.findCommentCount(ENTITY_TYPE_COMMENT, comment.getId()));
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments", commentVoList);
        return "/site/discuss-detail";
    }
```

## 添加评论

增加评论的同时，需要更新帖子表中的评论数量，还是按照三层结构开发。

### 开发数据层

* 增加评论

```xml
<insert id="insertComment" parameterType="Comment">
    insert into comment(<include refid="insertFields"></include>)
    values(#{userId},#{entityType},#{entityId},#{targetId},#{content},#{status},#{createTime})
</insert>
```

* 更新帖子评论数

```xml
<update id="updateCommentCount">
    update discuss_post set comment_count = #{commentCount} where id = #{id}
</update>
```

### 开发服务层

* 添加评论

```java
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        //添加完评论后，需要更新评论数量
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        int rows = commentMapper.insertComment(comment);
        //如果更新的是帖子
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            int count = commentMapper.selectCountByEntity(ENTITY_TYPE_POST, comment.getEntityId());
            discussPostService.updateCommentCount(comment.getEntityId(), count);
        }
        return rows;
    }
```

* 更新帖子数量

```java
public int updateCommentCount(int id, int commentCount) {
    return discussPostMapper.updateCommentCount(id, commentCount);
}
```

### 开发控制层

```java
@RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
    //统一的异常处理（如果空）
    //统一权限认证
    comment.setUserId(hostHolder.getUser().getId());
    comment.setStatus(0);
    comment.setCreateTime(new Date());
    commentService.addComment(comment);
    return "redirect:/discuss/detail/" + discussPostId;
}
```

## 私信列表

指的是朋友私信：

会话是指和某个用户的多条私信，私信列表是会话列表。会话列表只会显示最新的一条私信，而会话详情会包括和某个用户的全部私信。

还需要显示总的会话消息量。

会话ID，小的ID在前面大的ID在后面，为了查询会话数据时，筛选方便。

新建类：



### 数据层

