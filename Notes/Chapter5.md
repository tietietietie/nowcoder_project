# Chapter 5

## 消息队列服务器

服务器发送消息给某一用户，而且操作频繁

### 阻塞队列

JAVA自带API，解决线程通信问题，依靠put,take

生产者与消费者模式，Blocking Queue构建了一个桥梁，能够解决生产速度/消费速度不匹配问题。阻塞的时候只是在那里等着，但是不会占用CPU资源，对性能不会有影响。

Blocking Queue底层有多种实现方式，举例ArrayBlockingQueue。

分别定义一个生产者和三个消费者，其中生产者生产速度快，消费者消费速度慢，生产者和消费者共用一个阻塞队列

```java
public class BlockingQueueTests {

    public static void main(String[] args) {
        BlockingQueue<Integer> blockingQueue = new ArrayBlockingQueue<>(10);
        new Thread(new Producer(blockingQueue)).start();
        new Thread(new Consumer(blockingQueue)).start();
        new Thread(new Consumer(blockingQueue)).start();
        new Thread(new Consumer(blockingQueue)).start();
    }

}

class Producer implements Runnable {
    private BlockingQueue<Integer> blockingQueue;

    public Producer(BlockingQueue<Integer> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }


    @Override
    public void run() {
        for (int i = 0; i < 100; i++) {
            try {
                Thread.sleep(20);
                blockingQueue.put(i);
                System.out.println(Thread.currentThread().getName() + "当前生产：" + i + "共" + blockingQueue.size() + "个。");
            } catch (Exception e) {
                e.getStackTrace();
            }
        }
    }
}

class Consumer implements Runnable {
    private BlockingQueue<Integer> blockingQueue;

    public Consumer(BlockingQueue<Integer> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(new Random().nextInt(1000));
                int i = blockingQueue.take();
                System.out.println(Thread.currentThread().getName() + "当前消费：" + i + "共" + blockingQueue.size() + "个。");
            } catch (Exception e) {
                e.getStackTrace();
            }
        }
    }
}
```

## Kafka入门

* 分布式的流媒体平台，包括消息系统，日志收集，用户行为追踪，流式处理。
* 特点：高吞吐量，消息持久化（存在硬盘上）（长久保存）（对硬盘的顺序读写）（速度快），高可靠性（分布式），高扩展性（简单配置可增加服务器）
* 消息队列实现方式：点对点（每个消息只会消费一次），发布订阅模式
* 术语：
  * Broker：服务器
  * Zookeeper：独立软件，管理集群，Kafka可内置Zookeeper
  * Topic：空间，相当于文件夹，存放消息
  * Partition：将Topic分区，可以多线程同时写数据，
  * Offset，消息在partition中存放位置
  * Leader Replica：主副本，每个分区都有多个副本，可以提供消息
  * Leader Replica：从副本，只是备份，不负责相应，主副本挂掉，选择一个从副本变为主副本。
* 安装过程略

启动zookeeper

```
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```

启动kafka

```
bin\windows\kafka-server-start.bat config\server.properties
```

创建主题（表示一种消息类别）

```

```



## Spring整合Kafka

生产者发消息：主动

消费者收到消息：被动

演示：

```java
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class KafkaTests {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Autowired
    private KafkaConsumer kafkaConsumer;

    @Test
    public void testKafka() {
        kafkaProducer.sendMessage("test", "你好");
        kafkaProducer.sendMessage("test", "hello");
        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

@Component
class KafkaProducer {
    @Autowired
    private KafkaTemplate kafkaTemplate;

    public void sendMessage(String topic, String context) {
        kafkaTemplate.send(topic, context);
    }
}

@Component
class KafkaConsumer {
    @KafkaListener(topics = {"test"})
    public void handleMessage(ConsumerRecord consumerRecord) {
        System.out.println(consumerRecord.value());
    }
}
```

## 发布系统通知

点赞/评论/关注后，需要发布通知，系统发布通知是很频繁的功能。

三类不同的topic，封装，事件发生后，生产者产生消息，消费者从队列中读到消息，从message表中写一条数据。

* 封装事件对象，对所需的数据进行封装（消费者可以用多种方式处理）
* 开发生产者（产生事件）
* 开发消费者（消费事件，放在message库中）

补充：回复一共有三种：给帖子的回复，EntityType=1，给评论的回复：EntityType=2，给回复的回复：EntityType = 2，并且有targetId

### 封装事件

```java
public class Event {

    private String topic;
    private int userId;
    private int entityType;
    private int entityId;
    private int entityUserId;
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
```

### 定义生产者和消费者

生产者

```java
@Component
public class EventProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    //处理事件
    public void fireEvent(Event event) {
        //将之间发送指定主题
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
```

消费者：

```java
@KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
public void handleCommentMessage(ConsumerRecord record) {
    if (record == null || record.value() == null) {
        logger.error("消息内容为空");
        return;
    }
    Event event = JSONObject.parseObject(record.value().toString(), Event.class);
    if (event == null) {
        logger.error("消息格式错误");
        return;
    }
    //构造meaage对象
    Message message = new Message();
    message.setFromId(SYSTEM_USER_ID);
    message.setToId(event.getEntityUserId());
    message.setConversationId(event.getTopic());
    message.setCreateTime(new Date());
    Map<String, Object> content = new HashMap<>();
    content.put("userId", event.getUserId());
    content.put("entityType", event.getEntityType());
    content.put("entityId", event.getEntityId());
    if (!event.getData().isEmpty()) {
        for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
            content.put(entry.getKey(), entry.getValue());
        }
    }
    message.setContent(JSONObject.toJSONString(content));
    messageService.addMessage(message);
}
```

### 在关注/点赞/评论的controller中，生产对应事件

```java
Event event = new Event()
    .setTopic(TOPIC_COMMENT)
    .setEntityType(comment.getEntityType())
    .setEntityId(comment.getEntityType())
    .setUserId(hostHolder.getUser().getId())
    .setData("postId", discussPostId);
if (comment.getEntityType() == ENTITY_TYPE_POST) {
    DiscussPost post = discussPostService.findDiscussPostById(comment.getEntityId());
    event.setEntityUserId(post.getUserId());
} else {
    Comment target = commentService.findCommentById(comment.getEntityId());
    event.setEntityUserId(target.getUserId());
}
```

```java
if (likeStatus == 1) {
    Event event = new Event()
        .setTopic(TOPIC_LIKE)
        .setUserId(hostHolder.getUser().getId())
        .setEntityType(entityType)
        .setEntityId(entityId)
        .setEntityUserId(entityUserId)
        .setData("postId", postId);
    eventProducer.fireEvent(event);
}
```

```java
Event event = new Event()
    .setTopic(TOPIC_FOLLOW)
    .setEntityType(entityType)
    .setEntityId(entityId)
    .setEntityUserId(entityId);
eventProducer.fireEvent(event);
```

