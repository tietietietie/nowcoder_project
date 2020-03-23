# 项目学习笔记

## 初识Spring Boot

### 项目介绍

#### 功能介绍

某课网讨论区开发，包括发帖，私信，用户注册，网站流量统计，点赞，关注等功能。

#### 技术介绍

Java web的主流框架：Spring（事实上的行业标准）（学习重点）

一般的项目需求，都能在Spring中找到解决方案。

Spring Boot：可以简化Spring框架。

Spring MVC：处理请求

MyBatis：数据库(用Spring整合)

SSM:Spring+Spring MVC+MyBatis.(常用功能都可以实现)

Redis:No-server数据库,内存中数据库,性能好,提高服务性能.

Kafka:消息队列(进行消息发布)

Elasticsearch:流行的搜索引擎

上三者都能提高性能,都可以用Spring整数.

Spring Security, Spring Actuator:安全和系统状态统计.

#### 工具介绍

Maven:项目构建

集成开发工具:IntelliJ IDEA

数据库:MySQL+Redis

服务器:Tomcat

版本控制:Git

###  开发环境搭建

注意事项:

1. java --version:>= 8.0
2. 路径名全部为英文且不许有空格.

#### Maven

作用:构建项目+管理jar包

Maven仓库:

* 本地仓库:存放构件的位置,~/.m2/repository
* 远程仓库:中央仓库,镜像仓库(访问比较快),私服仓库(公司自己用的仓库)
* 本地仓库和远程仓库的联系:现在本地查找,在从远程查找所需要的包.

下载:

binary:不提供源码,zip供Windows使用

初始化一个Maven项目，使用如下命令行：

>  mvn archetype:generate -DgroupId=com.mycompany.app -DartifactId=my-app -DarchetypeArtifactId=maven-archetype-quickstart -DarchetypeVersion=1.4 -DinteractiveMode=false

Dgruopid：为公司名，通常是域名的倒序。

Dartifactid：当前项目的id

Darchetype...：项目模板

Darchetype..：项目版本

还可以设置是否交互，选择否（因为交互模式需要每次确认是/否)

**遇到的问题**

1. 在setting.xml中设置了镜像仓库地址，但是报错不能访问，后改正直接访问中央仓库。
2. 卡在Generating project in Batch mode这一步，参考[这里](https://www.cnblogs.com/wardensky/p/4513372.html)解决。

