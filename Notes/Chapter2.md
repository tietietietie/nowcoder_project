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