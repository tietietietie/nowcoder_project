# Problems

## Chapter 1

### 1.不能访问阿里云镜像

在settings-->Maven-->importing-->VM importer options处添加如下代码，修改安全协议。

> -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true

### 2.ctrl+函数名不能访问其源代码

热键冲突，在settings-->keymap-->main view --> navigation中找到declaration，发现其快捷键其实是ctrl+alt+鼠标右键。

### 3.编译Test项目时，卡在Resolving Maven dependencies

在settings-->Maven-->importing-->VM importer options修改JVM参数为 -Xms1024m -Xmx2048m。修改了heap值。

### 4.idea找不到new interface

点new-->class，然后选interface

### 5.修改了代码没有生效

忘记recompile直接run了，点击build--->rebuild project即可。

### 6.如何快速处理异常（快捷方式）

参考[这里](https://blog.csdn.net/a200822146085/article/details/92805214)，快捷键是alt+enter

### 7.无法访问SpringMVC的静态资源

添加如下配置

```java
#静态资源
spring.mvc.static-path-pattern=/static/**
spring.resources.static-locations=classpath:/static
```

### 8.忘记了mysql的初始密码

删掉mysql然后重装，如何删除mysql参考[这里](https://jingyan.baidu.com/article/47a29f24ba369bc0142399db.html)

如何设置自己的密码(注意分号结尾)

```
alter user root@localhost identified by ‘wodemima';
```

### 9.generate快捷键失效

修改keymap的generate快捷键为alt+f12

### 10.报错Error creating bean with name 'dataSource' defined in class path resource

版本我兼容,我下载的是新版的springboot,使用的是旧版的mybatis和mysql jar包,更新Jar包Bug就没了,以后一定不要新旧版本混用.