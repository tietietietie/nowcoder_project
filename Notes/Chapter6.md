# Chapter 6

## Elasticsearch入门

### 简介

* 分布式，Restful风格（对http协议进行格式规定）
* 对各种类型的数据都可以检索
* 速度快，实时（搜索过程：提交数据 ---> 搜索引擎建立索引 ---> 搜索）（能在前两步实时）
* 便于扩展，PB级（因为是分布式）

### 术语

- ES可以看成特殊的数据库，因为ES里面要保存一份数据，所以有：索引（数据库database），类型（表table），文档（行row），字段（列column）等术语。在ES6.0以后，废弃类型，索引逐渐对应一张表
- 集群，节点，分片（一个索引（表）的进一步划分，并发能力提高），副本（对分片的备份）

### 安装/使用

* ES，最好安装和Spring Boot一致的版本号

* 中文分词插件，在elasticsearch下的plugin文件新建ik文件夹，在此解压缩，想要新增分词，可以在config目录下的对应配置文件。

* postman能够存数据/搜索数据，而不用自己写很长的一串命令

#### 启动ES

在Bin里面点击相应bat文件即可

## Spring整合ES

* 引入依赖
* 配置文件
* 使用Spring的API，ElasticsearchTemplate或者ElasticsearchRepository（接口方式）。

### 引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

### 配置文件

```
#ElasticsearchProperties
spring.data.elasticsearch.cluster-name=nowcoder
spring.data.elasticsearch.cluster-nodes=127.0.0.1:9300
```

### 演示ElasticsearchRepository

1，对需要搜索的数据（文档）注释

```java
@Document(indexName = "discusspost", type = "_doc", shards = 6, replicas = 3)
public class DiscussPost {
    @Id
    private int id;

    @Field(type = FieldType.Integer)
    private int userId;
    //互联网招聘  尽可能地拆分更多关键词，从而增加搜索范围
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;
    @Field(type = FieldType.Integer)
    private int type;
    @Field(type = FieldType.Integer)
    private int status;
    @Field(type = FieldType.Date)
    private Date createTime;
    @Field(type = FieldType.Integer)
    private int commentCount;
    @Field(type = FieldType.Double)
    private double score;
```

2.在DAO数据层从定义接口

```java
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {
}
```

