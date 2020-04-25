# 第二章：开发社区的登录模块

## 发送邮件

目的：服务器向用户发送邮件

### 邮箱设置

* 申请了一个新浪邮箱，设置STMP服务开启。
* 安装Spring mail的jar包
* 配置邮箱

```
#MailProperties
spring.mail.host=smtp.sina.com
spring.mail.port=465
spring.mail.username=nowcoderzt@sina.com
spring.mail.password=0dba281fa3487473
spring.mail.protocol=smtps
spring.mail.properties.mail.smtp.ssl.enable=true
```

* 创建一个MailSender类

```java
@Component
public class MailClient {
    private static final Logger logger = LoggerFactory.getLogger(MailClient.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public void sendMail(String to, String subject, String context) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(context, true);
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("发送邮件失败" + e.getMessage());
        }
    }
}
```

测试邮件发送成功

### Spring Email

### 模板引擎

发送的是Html格式的邮件

在测试类中，需要我们主动调用模板引擎，注入相应bean即可，代码如下：

```java
    @Test
    public void testHtmlMail() {
        Context context = new Context();
        context.setVariable("username", "sunday");
        String content = templateEngine.process("/mail/demo", context);
        System.out.println(content);
        mailClient.sendMail("zhangtiezhangtie@outlook.com", "HTML", content);
    }
```

其中，需要指定模板文件的地址，及模板所需要的参数，模板引擎会自动生成网页（一个字符串content），把这个字符串使用mailClient发送即可。

## 开发注册功能

web项目：按照请求拆解功能，比如注册功能：

1，打开注册网页

2，把注册的信息发送给服务器（点注册）

3，把激活邮件发送给邮箱

4，利用激活链接打开网页

每一次请求都是先开发数据访问层，在开发业务层，最后开发视图层（三层架构），但是每一次请求不一定要用到这三层

### 打开注册页面

* 使用thymeleaf实现一段标签的复用

把index的顶部标签复用给register.html，部分代码如下：

````html
//在Index的header部分添加th代码
<div class="nk-container">
    <!-- 头部 -->
    <header class="bg-dark sticky-top" th:fragment="header">
//在register部分添加如下th代码
<div class="nk-container">
    <!-- 头部 -->
    <header class="bg-dark sticky-top" th:replace="index::header">
````

注意th的变量需要用@{}括起来，不然会报错

### 提交注册数据

首先导入“判断字符串/集合空值”的包

把我们网站的链接做成可配置的。在properties中设置网站域名（本机）

写两个工具，可以生成随机字符串，以及加密工具。

写一个service类，帮助我们把传来的用户数据存到数据库里面，具体包括：用户查重，加密密码，发送用户激活码，代码如下：

```java
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id) {
        return userMapper.selectById(id);
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        //空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空，谢谢");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空");
            return map;
        }
        //验证账号
        User findUser = userMapper.selectByName(user.getUsername());
        if (findUser != null) {
            map.put("usernameMsg", "该账号已存在");
            return map;
        }
        //验证邮箱
        findUser = userMapper.selectByEmail(user.getEmail());
        if (findUser != null) {
            map.put("emailMsg", "该邮箱已存在");
            return map;
        }
        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreatTime(new Date());
        userMapper.insertUser(user);

        //发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        //http://localhost:15213/communityactivation/101/code
        String url = domain + contextPath + "/activation" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);
    }
}
```

写一个controller类，来进行前后端交互

* 注册工程后，页面跳转到首页（第三方页面：operate-result.html，显示页面在多少秒后跳转（转到首页））
* 在修改html时，需要给表单中的每一个获得的变量（账号名/邮箱等），指定参数名，和user中的参数名一一对应，这样spring mvc可以自动的创建user
* 回到错误页面，需要把这些错误信息还在register中显示，使用th:value = "@{//判断user是否为null}"

终于收到了激活邮箱。。

![image-20200425135341323](Chapter2.assets/image-20200425135341323.png)

### 激活账号

有多种情况：

* 成功激活（第一次）
* 重复激活
* 无效的激活链接

![image-20200425135323665](Chapter2.assets/image-20200425135323665.png)