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

3.演示如何使用repository和template来插入数据，搜素数据

```java
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTest {
    //数据来源是Mysql
    @Autowired
    private DiscussPostMapper discussMapper;
    @Autowired
    private DiscussPostRepository discussRepository;
    @Autowired
    private ElasticsearchTemplate elasticTemplate;

    //往ES中添加数据
    @Test
    public void testInsert() {
        discussRepository.save(discussMapper.selectDiscussPostById(241));
        discussRepository.save(discussMapper.selectDiscussPostById(242));
        discussRepository.save(discussMapper.selectDiscussPostById(243));
    }

    //插入多条数据
    @Test
    public void testInsertList() {
        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100));
    }

    //数据修改
    @Test
    public void testUpdate() {
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人，哈哈哈哈哈哈哈");
        discussRepository.save(post);
    }

    //数据删除
    //略

    //搜索
    @Test
    public void testSearchByRepository() {
        //排序，分页，高亮显示
        //文本显示在网页的时候，需要加样式
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        //两份数据，底层获取到了高亮显示的值，但是没有处理
        //所以没有高亮数据
        //所以不如直接template
        Page<DiscussPost> page = discussRepository.search(searchQuery);
        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }
	//template搜素（repository底层起始就是调用template，但是不能返回带标签的结果，需要我们自己来定义一个SearchResultMapper
    @Test
    public void testSearchByTemplate() {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() <= 0) {
                    return null;
                }

                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {
                    DiscussPost post = new DiscussPost();

                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮显示的结果,返回的只是匹配到的一小段
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                return new AggregatedPageImpl(list, pageable, hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore()) {
                };
            }

            @Override
            public <T> T mapSearchHit(SearchHit searchHit, Class<T> aClass) {
                return null;
            }
        });
        System.out.println(page.getTotalElements());
        System.out.println(page.getTotalPages());
        System.out.println(page.getNumber());
        System.out.println(page.getSize());
        for (DiscussPost post : page) {
            System.out.println(post);
        }
    }
}
```

## 开发搜索功能

分为以下三个功能

* 搜索服务
  * 保存帖子/删除帖子/搜索帖子
* 发布事件
  * 发布帖子时，异步的将帖子移交到ES服务器
  * 增加评论时，异步的将帖子移交到ES服务器
  * 在消费组件增加一个方法，消费帖子
* 显示结果

### 服务层

使用之前discussRepository的代码,实现帖子的增加/删除/搜索

```java
@Service
public class ElasticsearchService {

    @Autowired
    private DiscussPostRepository discussRepository;

    @Autowired
    private ElasticsearchTemplate elasticTemplate;


    public void saveDiscussPost(DiscussPost post) {
        discussRepository.save(post);
    }

    public void deleteDiscussPost(int id) {
        discussRepository.deleteById(id);
    }

    public Page<DiscussPost> searchDiscussPost(String keyword, int current, int limit) {
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(current, limit))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();
        Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                SearchHits hits = response.getHits();
                if (hits.getTotalHits() <= 0) {
                    return null;
                }

                List<DiscussPost> list = new ArrayList<>();
                for (SearchHit hit : hits) {
                    DiscussPost post = new DiscussPost();

                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    // 处理高亮显示的结果,返回的只是匹配到的一小段
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null) {
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null) {
                        post.setContent(contentField.getFragments()[0].toString());
                    }

                    list.add(post);
                }

                return new AggregatedPageImpl(list, pageable, hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore()) {
                };
            }

            @Override
            public <T> T mapSearchHit(SearchHit searchHit, Class<T> aClass) {
                return null;
            }
        });
        return page;
    }
}
```

### 控制层

在commentController和discussPostController中激活事务，在eventConsumer中统一的更新ES数据库的帖子数据。

```java
//消费发帖事件
@KafkaListener(topics = {TOPIC_PUBLISH})
public void handlePublishMessage(ConsumerRecord record) {
    if (record == null || record.value() == null) {
        logger.error("消息内容为空");
        return;
    }
    Event event = JSONObject.parseObject(record.value().toString(), Event.class);
    if (event == null) {
        logger.error("消息格式错误");
        return;
    }
    DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
    elasticsearchService.saveDiscussPost(post);
}
```

新建searchController，对搜索到的帖子进行封装

```java
    //路径后面带问号传
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        //搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResults = elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());
        //聚合数据
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if (searchResults != null) {
            for (DiscussPost post : searchResults) {
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                map.put("user", userService.findUserById(post.getUserId()));
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);

        //实现分页
        page.setPath("/search?keyword=" + keyword);
        page.setRows(searchResults == null ? 0 : (int) searchResults.getTotalElements());
        return "/site/search";
    }
}
```

