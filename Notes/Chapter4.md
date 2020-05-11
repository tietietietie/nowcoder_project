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

