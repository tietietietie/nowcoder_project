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

