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



## 会话管理

### 定义

http是无状态且有会话的。

无状态：请求之间是没有联系的（无差别对待）（用户没有办法进行连续的交互）（服务器无法记住浏览器的状态）

如何解决业务之间的联系：使用cookies来解决无状态，形成整体。

所以有会话是指：使得服务器和浏览器之间能有连续的交互。使得服务器能够**记住**浏览器。

cookies：由服务器发送给浏览器（在响应头），表明其身份，浏览器保存到本地，下次浏览器携带着cookies再次访问服务器时（在请求头），服务器能够**认识**这个浏览器。所以cookies是一个特殊的数据.(很小的数据，只有一对key:value)（能够得到浏览器的一些特征数据）



### 演示cookies

为访问此路径的浏览器生成cookie并放在response中，代码如下：

```java
    //cookies演示
    @RequestMapping(path = "cookie/set", method = RequestMethod.GET)
    @ResponseBody
    public String setCookie(HttpServletResponse response) {
        //创建cookie
        Cookie cookie = new Cookie("code", CommunityUtil.generateUUID());
        //设置cookie的生效范围（只有在哪些路径有效）
        cookie.setPath("/community/alpha");
        //设置cookie的生存时间（因为默认是存在内存里的，关掉浏览器就没了）(单位是秒）
        cookie.setMaxAge(60 * 10);
        response.addCookie(cookie);
        return "set cookie";
    }
```

如何使用浏览器发来的cookie，如下：

```java
    @RequestMapping(path = "cookie/get", method = RequestMethod.GET)
    @ResponseBody
    public String getCookie(@CookieValue("code") String code) {
        System.out.println(code);
        return "get cookie";
    }
```

优点：弥补http的无状态

缺点：

* 不安全，因为不知道浏览器所在系统的安全性
* 对流量/性能有影响

### 演示Session

另一种解决办法：session

* 用于在服务端的存储在客服端信息，优点是安全，缺点是造成了服务器的压力。
* 与cookie是有关系的

1，浏览器访问服务器，服务器创建一个对应的session对象，session依赖于cookie，自动创建了sessionID放在cookie中，发送给服务器

2，浏览器把存的cookie的sessionID，在下次请求中发送给服务器，从而服务器可以通过这个sessionID，找到与此浏览器对应的session对象

演示如何创建session，传入数据

```java
    //session示例
    @RequestMapping(path = "session/set", method = RequestMethod.GET)
    @ResponseBody
    public String setSession(HttpSession session) {
        //SpringMVC会自动地注入，只需要声明即可
        session.setAttribute("id", 1);
        session.setAttribute("name", "test");
        return "set session";
    }
```



![image-20200425214211535](Chapter2.assets/image-20200425214211535.png)



服务端获得对应地seesion参数值以及sessionID

```java
    @RequestMapping(path = "session/get", method = RequestMethod.GET)
    @ResponseBody
    public String getSession(HttpSession session) {
        System.out.println(session.getId());
        System.out.println(session.getAttribute("name"));
        return "get session";
    }
```

所以session也能解决会话问题。

Q&A

* 在分布式部署中，为什么不使用session呢？

因为服务器是分布式部署的（承载大流量），浏览器访问的是服务器代替nginx，nginx按照负载均衡的原则分配请求。在多次请求的过程中，可能会有多台服务器处理同一个浏览器的请求，但session只保存在一台服务器中。

解决策略：

粘性session：浏览器只访问一个服务器。（负载不均衡）

同步session：每个服务器存的session都相同（性能降低，增大服务器的耦合性）

共享session：单独有一台服务器来存储session，当服务器需要使用session时，往这台服务器申请（这台服务器威胁到了整个集群）

常用解决方案：一般数据都使用cookie而不是session，必要重要的数据存到数据库（集群）（非关系型数据库redis）里。

## 生成验证码

使用现成工具：kaptcha，在服务端内存中画出验证码，发送给浏览器。

1. 导入kaptcha的jar包
2. 编写Kaptcha的配置类
3. 生成随机字符，生成图片

配置Kaptcha如下：

```java
@Configuration
public class KaptchaConfig {
    @Bean
    public Producer kaptchaProducer() {
        Properties properties = new Properties();
        properties.setProperty("kaptcha.image.width", "100");
        properties.setProperty("kaptcha.image.height", "40");
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0");
        properties.setProperty("kaptcha.textproducer.char.string", "1234567890qwertyuiopsdfghklzxcnvbm");
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        properties.setProperty("kaptcha.noise,impl", "com.google.code.kaptcha.impl.NoNoise");
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }
```

生成验证码，如下：

```java
@RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session) {
        //生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        //保存验证码到session
        session.setAttribute("kaptcha", text);

        //将图片输出到浏览器
        response.setContentType("image/png");

        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败" + e.getMessage());
        }
```

login.html修改如下：

```html
<div class="col-sm-4">
    <img th:src="@{/kaptcha}" id="kaptcha" style="width:100px;height:40px;" class="mr-2"/>
    <a href="javascript:refresh_kaptcha();" class="font-size-12 align-bottom">刷新验证码</a>
</div>
```

```javascript
function refresh_kaptcha(){
    var path = "/community/kaptcha?p=" + Math.random();
    $("#kaptcha").attr("src",path);
}
```

