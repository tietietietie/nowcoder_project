# Chapter 4

## Redies入门

NoSQL：不只是SQL

Redis按照键值对存储，其中Key为String，Value支持多种数据结构

Redis将所有数据存放在内存，同时将数据按照快照（完全存储，恢复快）（耗时，阻塞）（不适合实时）/日志（AOF）（存命令）（实时）（体积大）（恢复速度慢）的形式保存在内存上，保证安全

使用简单，常用场景：缓存，排行榜（热门帖子缓存），计数器（频繁更新访问量），社交网络（点赞/关注），消息队列

[官网](https://redis.io/)，学会[操作数据的命令](https://redis.io/commands)就可以使用了。

## Spring整合Redis

步骤：

* 引入依赖spring-boot-starter-data-redis
* 配置Redis，配置数据库参数，编写配置类，构造RedisTemplate
* 访问Redis，使用redisTemplate.opsForValue()等函数

### 导入jar包

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
    <version>2.2.7.RELEASE</version>
</dependency>
```

### 配置redis

配置文件：

```xml
#redis
spring.redis.database=11
spring.redis.host=localhost
spring.redis.port=6379
```

配置类：

```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        //注入连接工厂
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        //设置key的序列化方式
        template.setKeySerializer(RedisSerializer.string());
        //设置普通的value的序列化方式
        template.setValueSerializer(RedisSerializer.json());
        //设置hash的key的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        //设置hash的value的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());
        //配置完后需要触发？？
        template.afterPropertiesSet();
        return template;
    }
}
```

### 测试redis

测试Value类型访问方式：

```java
@Test
public void testStrings() {
    String redisKey = "test:count";
    redisTemplate.opsForValue().set(redisKey, 1);
    System.out.println(redisTemplate.opsForValue().get(redisKey));
    System.out.println(redisTemplate.opsForValue().increment(redisKey));
    System.out.println(redisTemplate.opsForValue().decrement(redisKey));
}
```

测试Hash类型的访问方式：

```java
@Test
public void testHashes() {
    String redisKey = "test:hash";
    redisTemplate.opsForHash().put(redisKey, "id", 1);
    redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");
    System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
    System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
}
```

测试List类型访问方法：

```java
@Test
public void testLists() {
    String redisKey = "test:list";
    redisTemplate.opsForList().leftPush(redisKey, 101);
    redisTemplate.opsForList().leftPush(redisKey, 102);
    redisTemplate.opsForList().leftPush(redisKey, 103);
    System.out.println(redisTemplate.opsForList().size(redisKey));
    System.out.println(redisTemplate.opsForList().index(redisKey, 1));
    System.out.println(redisTemplate.opsForList().range(redisKey, 0, 2));
    System.out.println(redisTemplate.opsForList().leftPop(redisKey));
    System.out.println(redisTemplate.opsForList().leftPop(redisKey));
    System.out.println(redisTemplate.opsForList().leftPop(redisKey));
}
```

测试Set类型访问方法：

```java
@Test
public void testSets() {
    String redisKey = "test:teachers";
    redisTemplate.opsForSet().add(redisKey, "刘备", "Andy", "Selena", "Shawn");
    System.out.println(redisTemplate.opsForSet().size(redisKey));
    System.out.println(redisTemplate.opsForSet().pop(redisKey));
    System.out.println(redisTemplate.opsForSet().members(redisKey));
}
```

测试排序set的访问方法

```java
@Test
public void testSortedSets() {
    String redisKey = "test:students";
    redisTemplate.opsForZSet().add(redisKey, "Linda", 92);
    redisTemplate.opsForZSet().add(redisKey, "John", 86);
    redisTemplate.opsForZSet().add(redisKey, "Thomas", 98);
    redisTemplate.opsForZSet().add(redisKey, "Austin", 73);
    redisTemplate.opsForZSet().add(redisKey, "Ben", 64);

    System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
    System.out.println(redisTemplate.opsForZSet().score(redisKey, "John"));
    System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "John"));
    System.out.println(redisTemplate.opsForZSet().range(redisKey, 0, 2));
    System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0, 2));
}
```

操作key的方法：

```java
@Test
public void testKeys() {
    redisTemplate.delete("test:hash");
    System.out.println(redisTemplate.hasKey("test:hash"));
    redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
}
```

当对某个key需要多次操作时，可以绑定

```java
@Test
public void testBoundOperations() {
    String redisKey = "test:count";
    BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
    operations.increment();
    operations.increment();
    operations.increment();
    operations.increment();
    System.out.println(operations.get());
}
```

Redis支持事务，redis将事务中所有操作入队，然后一次性执行，所以事务中一般不含有查询操作，并且使用编程式事务

```java
@Test
public void testTransactional() {
    Object obj = redisTemplate.execute(new SessionCallback() {
        @Override
        public Object execute(RedisOperations redisOperations) throws DataAccessException {
            String redisKey = "test:tx";
            //启用事务
            redisOperations.multi();
            redisOperations.opsForSet().add(redisKey, "Hello");
            redisOperations.opsForSet().add(redisKey, "World");
            //此查询无效，因为之前添加数据的操作都还没执行
            System.out.println(redisOperations.opsForSet().members(redisKey));
            //提交事务
            return redisOperations.exec();
        }
    });
    System.out.println(obj);
}
```

## 点赞

点赞为高频操作，可以使用redis存储数据，提高性能

* 对帖子点赞，对帖子的评论点赞
* 没赞点一下变成点赞，点赞了再点一下，变成没赞
* 需要统计点赞数量

### 数据访问层

不需要写，因为非常简单

### 服务层

写一个工具，专门生成redis的key

```java
public class RedisKeyUtil {
    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity";

    //某个实体的赞
    //like:entity:entityType:entityId -> set(userId)
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

}
```

点赞/查询点赞数量/查询点赞状态

```java
@Service
public class LikeService {
    @Autowired
    private RedisTemplate redisTemplate;

    //点赞
    public void like(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        if (redisTemplate.opsForSet().isMember(entityLikeKey, userId)) {
            redisTemplate.opsForSet().remove(entityLikeKey, userId);
        } else {
            redisTemplate.opsForSet().add(entityLikeKey, userId);
        }
    }

    //查询实体点赞数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    //查询某人对某实体是否点过赞(int更具扩展性）
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }
}
```

### 控制层

采用异步消息，进行点赞，返回点赞数量和点赞状态

```java
@Controller
public class LikeController {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(path = "/like", method = RequestMethod.POST)
    @ResponseBody
    public String like(int entityType, int entityId) {
        User user = hostHolder.getUser();
        likeService.like(user.getId(), entityType, entityId);
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);
        return CommunityUtil.getJSONString(0, null, map);
    }

}
```

## 我收到的赞

* 个人主页上显示

重构点赞功能，点赞时，把点赞对象（用户）的赞加一。

* 开发个人主页

包括个人信息，以及收到的赞

### 修改RedisKeyUtil

每一个用户获得的赞，作为一个key存在redis中

```java
private static final String PREFIC_USER_LIKE = "like:user";
//某个用户收获的赞
public static String getUserLikeKey(int userId) {
    return PREFIC_USER_LIKE + SPLIT + userId;
}
```

### 修改likeService

需要保证事务性

```java
public void like(int userId, int entityType, int entityId, int entityUserId) {
    //        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
    //        if (redisTemplate.opsForSet().isMember(entityLikeKey, userId)) {
    //            redisTemplate.opsForSet().remove(entityLikeKey, userId);
    //        } else {
    //            redisTemplate.opsForSet().add(entityLikeKey, userId);
    //        }
    //更新某用户的赞，需要保证事务性
    redisTemplate.execute(new SessionCallback() {
        @Override
        public Object execute(RedisOperations operations) throws DataAccessException {
            String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
            String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
            boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);
            //开启事务
            operations.multi();
            if (isMember) {
                operations.opsForSet().remove(entityLikeKey, userId);
                operations.opsForValue().decrement(userLikeKey);
            } else {
                operations.opsForSet().add(entityLikeKey, userId);
                operations.opsForValue().increment(userLikeKey);
            }
            return operations.exec();
        }
    });
}
```

### 修改Controller，点赞对象id作为参数传入

代码略

### 修改用户个人主页代码，显示赞

代码略

## 关注/取消关注

* 实现关注/取关功能
* 统计关注数/粉丝数

A关注B，A是B的粉丝，B是A的关注者，A是B的follower，B是A的followee

关注的目标不能写死，可以关注用户或者帖子等抽象实体

分为两部：实现关注/去关，统计关注数量，粉丝数量

### 定义RedisKeyUtil

```java
//某个用户的关注实体
//followee:userId:entityType -> zSet(entityId,time)
public static String getFolloweeKey(int userId, int entityType) {
    return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
}

//某个用户的粉丝
//follower:entityType:entityId -> zSet(userId,time)
public static String getFollowerKey(int entityType, int entityId) {
    return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
}
```

### 服务层

关注/点赞，查询关注者数量，查询粉丝数量，查询当前用户对某用户的关注状态

```java
@Service
public class FollowService {
    @Autowired
    private RedisTemplate redisTemplate;

    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                return operations.exec();
            }
        });
    }

    public void unFollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);
                return operations.exec();
            }
        });
    }

    //查询关注实体的数量
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().size(followeeKey);
    }

    //查询粉丝数量
    public long fingFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().size(followerKey);
    }

    //显示关注状态
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        Double status = redisTemplate.opsForZSet().score(followeeKey, entityId);
        return status != null;
    }
}
```

### 控制层

在主页显示关注数量/粉丝数量/关注状态，并定义“关注”按钮

```java
@Controller
public class FollowController {
    @Autowired
    private FollowService followService;
    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.follow(user.getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0, "已关注");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.unFollow(user.getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0, "已取消关注");
    }
}
```

```java
@Controller
public class FollowController {
    @Autowired
    private FollowService followService;
    @Autowired
    private HostHolder hostHolder;

    @RequestMapping(path = "/follow", method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String follow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.follow(user.getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0, "已关注");
    }

    @RequestMapping(path = "/unfollow", method = RequestMethod.POST)
    @ResponseBody
    @LoginRequired
    public String unfollow(int entityType, int entityId) {
        User user = hostHolder.getUser();
        followService.unFollow(user.getId(), entityType, entityId);
        return CommunityUtil.getJSONString(0, "已取消关注");
    }
}
```

## 关注列表/粉丝列表

已有有数据，展示即可。

业务层：查询某用户关注的人，以及粉丝，支持分页

表现层：处理请求，编写模板

### 服务层

获得某一用户的关注者/关注事件，粉丝/关注之间，放在map中，以list形式返回

```java
    //查询某个用户关注的人
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        if (targetIds != null && !targetIds.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Integer targetId : targetIds) {
                Map<String, Object> map = new HashMap<>();
                User user = userService.findUserById(targetId);
                map.put("user", user);
                Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
                map.put("followTime", new Date(score.longValue()));
                list.add(map);
            }
            return list;
        }
        return null;
    }

    //查询某用户的粉丝
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        //set虽然是无序的，但是redis做了内置处理，变得有序
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        if (targetIds != null && !targetIds.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Integer targetId : targetIds) {
                Map<String, Object> map = new HashMap<>();
                User user = userService.findUserById(targetId);
                map.put("user", user);
                Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
                map.put("followTime", new Date(score.longValue()));
                list.add(map);
            }
            return list;
        }
        return null;
    }
```

### 控制层

处理分页信息，以及判断当前用户是否关注了list中的用户,以及获得当前用户的信息

```java
    @RequestMapping(path = "/followees/{userId}", method = RequestMethod.GET)
    public String getFollowees(@PathVariable("userId") int userId, Model model, Page page) {
        User user = userService.findUserById(userId);
        model.addAttribute("user", user);
        page.setLimit(5);
        page.setPath("/followees/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));
        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (userList != null && !userList.isEmpty()) {
            for (Map<String, Object> map : userList) {
                User followee = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(followee.getId()));
            }
        }
        model.addAttribute("users", userList);
        return "site/followee";
    }

    private boolean hasFollowed(int userId) {
        if (hostHolder.getUser() == null) {
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
    }

    @RequestMapping(path = "/followers/{userId}", method = RequestMethod.GET)
    public String getFollowers(@PathVariable("userId") int userId, Model model, Page page) {
        User user = userService.findUserById(userId);
        model.addAttribute("user", user);
        page.setLimit(5);
        page.setPath("/followers/" + userId);
        page.setRows((int) followService.fingFollowerCount(ENTITY_TYPE_USER, userId));
        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userList != null && !userList.isEmpty()) {
            for (Map<String, Object> map : userList) {
                User follower = (User) map.get("user");
                map.put("hasFollowed", hasFollowed(follower.getId()));
            }
        }
        model.addAttribute("users", userList);
        return "site/follower";
    }
```

## 优化登录模块

* 使用redis存储验证码（因为频繁访问），而且不需要永久保存，存在session里有问题。
* Redis处理登录凭证（ticket），（每次在拦截器里面查）
* 缓存用户信息，每次根据凭证查用户

### Redis验证码

定义key

```java
//验证码
public static String getKaptchaKey(String owner) {
    return PREFIX_KAPTCHA + SPLIT + owner;
}
```

生成验证码所有者，放在cookie中（类似于sessionID）

```java
//需要解决验证码的归属问题
String kaptchaOwner = CommunityUtil.generateUUID();
Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
cookie.setMaxAge(60);
cookie.setPath(contextPath);
response.addCookie(cookie);

//将验证码存入redis
String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);
```

检查验证码

```java
//检查验证码
String kaptcha = null;
if (StringUtils.isNotBlank(kaptchaOwner)) {
    String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
    kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
}
```

### Redis记录登录凭证

redis key

```java
//登录凭证
public static String getTicketKey(String ticket) {
    return PREFIX_TICKET + SPLIT + ticket;
}
```

### Redis缓存用户信息

* 缓存时，如果取到，则取，不然，添加缓存，更新数据时，也要更新缓存（但是一般是直接删除，下次请求重查即可）（更新数据可能会有并发问题）。

  * 优先缓存取值
  * 取不到，初始化缓存
  * 数据变化，清除缓存

  在service层定义三个函数，分别初始化缓存，获得缓存，清除缓存

  ```java
  //从缓存中取数据
  private User getCache(int userId) {
      String redisKey = RedisKeyUtil.getUserKey(userId);
      return (User) redisTemplate.opsForValue().get(redisKey);
  }
  
  //初始化缓存
  private User initCache(int userId) {
      User user = userMapper.selectById(userId);
      String redisKey = RedisKeyUtil.getUserKey(userId);
      redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
      return user;
  }
  
  //清除缓存
  private void clearCache(int userId) {
      String redisKey = RedisKeyUtil.getUserKey(userId);
      redisTemplate.delete(redisKey);
  }
  ```

  