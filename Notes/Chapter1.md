# 初识Spring Boot

## 项目介绍

### 功能介绍

某课网讨论区开发，包括发帖，私信，用户注册，网站流量统计，点赞，关注等功能。

### 技术介绍

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

### 工具介绍

Maven:项目构建

集成开发工具:IntelliJ IDEA

数据库:MySQL+Redis

服务器:Tomcat

版本控制:Git

##  开发环境搭建

注意事项:

1. java --version:>= 8.0
2. 路径名全部为英文且不许有空格.

### Maven

作用:构建项目+**管理jar包**

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
3. 阅读Maven手册，[5分钟创建Maven](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)

编译：mvn compile：编译项目，生成在target文件夹里面的class中

mvn clean：将生成的target文件夹删除。

mvn test：在target中包含了test文件夹，test包含了compile。

### IntelliJ IDEA

#### 在IDEA中连接Maven

1. 找不到build tool里面的maven选项，因为没有安装maven插件。
2. 修改maven工作路径

![image-20200324100620717](Chapter1.assets/image-20200324100620717.png)

#### 遇到的问题

- 创建新Maven项目时，无法导入Maven文件，查看log，发现如下错误：

![image-20200324104640015](Chapter1.assets/image-20200324104640015.png)

解决办法：因为Maven版本太新，IDEA版本不支持，下载Maven 3.6.1

* IDEA如何清除缓存/项目：

删除缓存：可以现在Maven中搜索“cache...."可以删除缓存

删除项目：现在IDEA中移除项目，然后在对应的文件夹删除，注意**清空回收站**才算完全清除

#### 编译项目

IDEA右侧有Maven快捷命令，可以点击即可。

![image-20200324124228408](Chapter1.assets/image-20200324124228408.png)

还可以直接点:build-->build project

也可以直接在对应的.java文件右键，点击run（右键找不到run选项）

#### 快捷健

* 快速查找：按两次shift
* 快速找到接口需要实现的方法：ctrl+I

### Spring Boot

帮助优化Maven管理jar包，因为maven中的包搜索相对麻烦。

#### Maven搜索包

www.mvnrepository.com下载，然后复制对应的语句到pom文件的dependencies位置，Maven会自动下载。

#### Spring Initializer

使用Spring Initializer,可以把某个功能的包一次性下载下来。在start.spring.io中设置相关名称，及添加所需工具包即可。

我选择了Thymeleaf,Spring web,和Spring boot devTools三个依赖（AOP依赖没有找到）

得到一个文件压缩包，解压缩到某一地址，然后用IDEA打开即可。

#### 遇到的问题

* 无法下载插件

因为连接的是国外镜像，无法下载速度慢，需要设置为国内阿里云的镜像，但是会出现连接不上的问题，此时需要在settings-->Maven-->importing-->VM importer options处添加如下代码：

```
-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true
```

* 没法显示外部库

没有解决。。

#### 启动程序

Jar包里内置了tomcat，启动可以运行tomcat服务器，默认端口在8080。

#### 特点（核心价值）

1. 起步依赖：starter,帮我们自动添加很多依赖（dependencies）
2. 自动配置：减少很多配置
3. 端点监控：对服务器进行监控。

#### 示例

如何response浏览器发出的请求，在controller下编写对应代码，如下：

![image-20200324202406159](Chapter1.assets/image-20200324202406159.png)

在浏览器输入：localhost:8080/alpha/hello，即可返回sayHello()函数的返回值。

#### 修改服务器端口

```
server.port = 15213
server.servlet.context-path = /community
```

在resources的application.properties文件

会发现并不能生效，因为在target文件中也会存在着配置文件，修改那里的配置文件才会生效。

此时在浏览器输入：localhost:15213/community/alpha/hello可访问对应文件（or class)

## Spring入门

多套框架（Spring全家桶），包括：

Spring+Spring Boot+Spring Cloud(将项目拆分为若干个子项目/微服务）+Spring Cloud Data Flow（多种客服端集成数据）

文档手册：Spring.io

### Spring Framework

包括

* Spring core ：IoC
* Spring Data Access
* Web Servlet
* Integration

详细内容点[这里](https://spring.io/projects/spring-framework#overview)

#### Spring IoC

* 控制反转（与常见的[对象关系](https://blog.csdn.net/jiahao1186/article/details/82634723)不同）
* 依赖注入
* IoC容器：一个工厂，管理各种Bean和配置文件（需要提供两种数据，bean对象以及配置文件）（对象之间不会**直接产生关联，降低耦合度**）

More:Bean的详细解释见[这里](https://www.awaimai.com/2596.html)

**遇到的问题**

* 不显示external libraries

搜索文件：projectView.xml，修改其中的“showLibraryContents”为true。

* ctrl+鼠标右键不能进入函数源码

原因：热键冲突

解决办法：在settings-->keymap-->main view --> navigation中找到declaration，发现其快捷键其实是ctrl+alt+鼠标右键。

* 访问目标函数时，需要进行登录

原因：Spring Security在起作用

解决办法：在pom文件中注释掉Security的dependency即可。参考[这里](https://blog.csdn.net/qq_36079461/article/details/96759099)

More：IDEA注释快捷键：ctrl+shift+/

#### 怎样才能被容器扫描

容器会自动创建，但是哪些Bean会被扫描呢？在main函数中，我们传入SpringApplication的其实是一个配置文件：

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication //这个注解标识的类，其实是一个配置文件，项目启动的时候帮我们配置
public class CommunityApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}
```

其中，SpringBootApplication下存放着配置文件，可以放入IoC容器的Bean，哪些函数（Bean)会被放入IoC文件呢，前面有四种注解的函数，会被扫描：

* @Controller(处理请求)
* @Service（提供服务）
* @Repository(处理数据库)
* @Conponent(通用)

#### 演示IoC

在test中演示，如何获得容器，代码如下：

```java
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class CommunityApplicationTests implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Test
    public void testApplicationContext() {
        System.out.println(applicationContext);
    }
}
```

* ApplicationContextAware接口可以帮助我们实现容器的传递，在实现了serApplicationContext方法后，我们便可以通过其参数applicationcontext访问容器的
* 将容器的内容打印出来

#### 使用容器降低耦合度（生成bean）

举例：在项目中实现了AlphaDao接口，并且实现了一个类叫做AlphaDaoHibernateImpl，通过注解@repository，它可以被容器扫描并管理，项目中的其他函数调用时，都是基于容器getBean方法，某一天，需要将此接口的技术升级，实现类变成AlphaDaoMyBatisImpl，此时只需要在这个类加上@Primary，即可实现此接口的升级（面向接口）,因为此时向容器中寻找AlphaBao类，默认会给你AlphaDaoMyBatisImpl。

```java
@Repository
@Primary
public class AlphaDaoMyBatisImpl implements AlphaDao {
    @Override
    public String select() {
        return "MyBatis";
    }
}
```

```java
@Test
public void testApplicationContext() {
    System.out.println(applicationContext);
    AlphaDao alphaDao = applicationContext.getBean(AlphaDao.class);
    System.out.println(alphaDao.select());
}
```

如果想要访问特定的bean，则可以通过名字，强制返回指定bean

```java
@Repository("alphaHibernate")
public class AlphaDaoHibernateImpl implements AlphaDao {
    @Override
    public String select() {
        return "Hibernate";
    }
}
```

```java
@Test
public void testApplicationContext() {
    System.out.println(applicationContext);
    AlphaDao alphaDao = applicationContext.getBean(AlphaDao.class);
    System.out.println(alphaDao.select());
    alphaDao = applicationContext.getBean("alphaHibernate", AlphaDao.class);
    System.out.println(alphaDao.select());
}
```

#### 容器管理bean的方法

除了自动构建bean外，还可以初始化bean,以及销毁bean，其中初始化函数是在对象构造函数调用之后，销毁函数是在对象销毁之前，具体如下所示：

```java
@Service
public class AlphaService {
    public AlphaService() {
        System.out.println("实例化AlphaService");
    }

    @PostConstruct
    public void init() {
        System.out.println("初始化AlphaService");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("销毁AlphaService");
    }
}
```

并且默认情况，容器内的bean只会被实例一次（单例模式），如果需要每次调用bean都要实例化，则在对应bean加上注解@Scope("prototype")

