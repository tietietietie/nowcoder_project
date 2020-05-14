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

