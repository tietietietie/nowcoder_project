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