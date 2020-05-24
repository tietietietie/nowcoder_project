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

### 3.不能显示用户上传的头像

文件路径名写错，忘记在增加的子路径前面加上"/"

### 4.找不到Main方法

>错误: 在类 com.nowcoder.community.util.CommunityUtil 中找不到 main 方法, 请将 main 方法定义为:
>   public static void main(String[] args)
>否则 JavaFX 应用程序类必须扩展javafx.application.Application

解决办法：

## Chapter 3

### 1.显示帖子评论的时候，模板引擎报错

原因：

```java
if (reply.getTargetId() != 0) {
	replyVo.put("target", userService.findUserById(reply.getTargetId()));
} 
```

因为当不存在回复对象target时，也要放入target

```java
if (reply.getTargetId() != 0) {
    replyVo.put("target", userService.findUserById(reply.getTargetId()));
} else
    replyVo.put("target", null);
```

### 2.显示帖子评论时，无法获得page的offset变量

原因：page的getOffset方法写成了page.getOffSet()。

### 3.写Mapper接口时，给参数声明Param注解会报错

```java
//新增消息
int insertMessage(@Param("message")Message message);
```

去掉注解不会报错，原因传入实体类型和传入基本类型，xml的写法是不同的，

```java
public List<student> selectuser(@Param(value = "page")int pn ,@Param(value = "st")student student);

<select id="selectuser" resultType="com.user.entity.student">
    SELECT * FROM student
    where sname like concat(concat("%",#{st.sname}),"%")
    LIMIT #{page} ,5
</select>
```

参考[这里](https://www.cnblogs.com/goloving/p/9241449.html)

## Chapter 5

### 1.kafka报错

>错误：
>
>Caused by: java.lang.IllegalStateException: Topic(s) [comment, follow, like] is/are not present and missingTopicsFatal is true

解决办法：修改application.properties

```xml
spring.kafka.listener.missing-topics-fatal=false
```

### 2.Aspect报错

aspect默认记录service的日志，并默认一定有request，但是在kafka使用controller时，并没有contoller

>错误：
>
>Caused by: java.lang.NullPointerException: null
>	at com.nowcoder.community.aspect.ServiceAspect.before(ServiceAspect.java:31)

解决办法：考虑空值情况

## Chapter 7

### 1.导入SecurityDemo是报错

解决办法：

不能用Idea导入，直接打开就可以了。。。

### 2.在设置异步请求的_csrf令牌时，前端报错

错误：

>jquery-3.3.1.min.js:2 Uncaught TypeError: Cannot read property 'toLowerCase' of undefined
>at Object.setRequestHeader (jquery-3.3.1.min.js:2)
>at HTMLDocument.<anonymous> (index.js:12)
>at HTMLDocument.dispatch (jquery-3.3.1.min.js:2)
>at HTMLDocument.y.handle (jquery-3.3.1.min.js:2)
>at Object.trigger (jquery-3.3.1.min.js:2)
>at Function.ajax (jquery-3.3.1.min.js:2)
>at Function.w.<computed> [as post] (jquery-3.3.1.min.js:2)
>at HTMLButtonElement.publish (index.js:19)
>at HTMLButtonElement.dispatch (jquery-3.3.1.min.js:2)
>at HTMLButtonElement.y.handle (jquery-3.3.1.min.js:2)

代码：

```javascript
$(function(){
	$("#publishBtn").click(publish);
});


function publish() {
	$("#publishModal").modal("hide");
	//发送AJAX请求前，需要带上CSRF令牌
    var token = $("mata[name='_csrf']").attr("content");
    var header = $("mata[name='_csrf_header']").attr("content");
    $(document).ajaxSend(function(e, xhr, options){
        xhr.setRequestHeader(header, token);
    });
	//先返回结果再显示
    //获取标题/内容
    var title =  $("#recipient-name").val();
    var content = $("#message-text").val();
    //发送异步请求
    $.post(
        "/community/discuss/add",
        {"title":title,"content":content},
        function(data){
            data = $.parseJSON(data);
            //提示框显示返回消息
            $("#hintBody").text(data.msg);
            //显示提示框/两秒后自动隐藏
            $("#hintModal").modal("show");
            setTimeout(function(){
                $("#hintModal").modal("hide");
                if(data.code == 0) {
                    window.location.reload();
                }
            }, 2000);
        }
    );
}
```

