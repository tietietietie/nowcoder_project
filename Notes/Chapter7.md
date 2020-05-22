# Chapter 7

* 提高系统性能
* 提高系统安全性

## Spring Security

* 提高身份**认证**和**授权**（帖子置顶/加精/删除）
* 方便扩展，自定义
* 防止各种攻击
* 可以和各种web开发集成（Spring MVC）

### 底层思路

Spring MVC ： DispatcherServlet ---> controller 一对多，其中拦截器可以在其中拦截请求，在DispatcherServlet之前，还有一个filter，可以对DispatcherServlet进行拦截，其中DispatcherServlet和filter都是javaEE的规范。

Spring Security利用Filter统一规范认证/授权，大概11个。（判断时机比较靠前）

Spring Security是Spring中比较难的，比较建议学习，对框架理解比较好。可以参考[这里](http://www.spring4all.com/article/428)的教程

### 演示Demo

打开demo，并导入Security组件，会自动为系统配置。

导入后，需要使用账号密码才能访问首页。账号为user，密码在服务端console可以看到

可以把这个登录页面换成自己的。