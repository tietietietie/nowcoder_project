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

版本我兼容,我下载的是新版的springboot,使用的是旧版的mybatis(2.1.2)和mysql(8.0.15) jar包,更新Jar包Bug就没了,以后一定不要新旧版本混用.

### 11.更新数据库表项时报错：org.apache.ibatis.binding.BindingException: Parameter 'idList' not found

参考[这里](https://blog.csdn.net/qq_28379809/article/details/83342196?depth_1-utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromBaidu-1&utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromBaidu-1)，因为参数默认是按照参数顺序从接口传到mapper的，需要添加@Param注解

### 12.链接mysql报错：Public Key Retrieval is not allowed

参考[这里](https://www.jianshu.com/p/a1d262143919)，在url加上allowPublicKeyRetrieval=true参数，

但是我打开mysql workbench的相应数据库后，Bug自动消失了(+_+)?

## Chapter 2

### 1.端口号被占用，程序已经启动

强制关闭端口对应的进程，在命令行中使用如下命令

```
netstat -ano|findstr "15213"
//可以确定对应PID
taskkill /pid 5692 -t -f
//将进程杀死
```

### 2.执行sql语句找不到传入的参数

原因：使用动态sql查询语句，必须对参数加注解

```java
    @Update({
            "<script> ",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket != null \" > ",
            "and 1 = 1 ",
            "</if> ",
            "</script>"
    })
    int updateStatus(@Param("ticket") String ticket, @Param("status") int status);
```

