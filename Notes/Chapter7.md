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

#### 需要将User实现UserDetails接口

根绝type来定义用户的权限（但是是字符串"USER""ADMIN"

```java
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    //凭证未过期
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    //账号可用
    public boolean isEnabled() {
        return true;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    //关键：权限
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (type){
                    case 1:
                        return "ADMIN";
                    default:
                        return "USER";
                }
            }
        });
        return list;
    }
```

#### 在UserService中实现UserDetailsService

实现此接口可以帮助Security来查询用户

```java
@Override
//和findUserByName的功能是一样的，但是实现这个方法，security可以帮助我们检查登录
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return this.findUserByName(username);
}
```

#### 配置SecurityConfig（重点）

包括如下配置：

* 忽略静态资源的权限判断
* 实现"账号-密码"认证类的配置
* 配置”认证“相关消息
  * 登录页面
  * 配置连接的权限
  * 在账号-密码filter前面增加一个filter
  * 转发 VS 重定向
  * 添加Remember-me功能

```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    private UserService userService;

    @Override
    public void configure(WebSecurity web) throws Exception {
        //忽略静态资源
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    //对认证处理，auth:认证核心结构，用于构造接口实例
    //常见实现类：provideManager为其默认实现类
    //没有验证码
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        //内置的认证规则
        //auth.userDetailsService(userService).passwordEncoder(new Pbkdf2PasswordEncoder("12345"));
        //自定义认证规则
        //有多种AuthenticationProvider，每一种访问一种认证
        //AuthenticationManagerBuilder自己不去做认证，委托给AuthenticationProvider
        auth.authenticationProvider(new AuthenticationProvider() {
            @Override
            //实现账号密码认证
            //authentication用于封装认证信息的接口，不同的实现类代表不同类型的认证信息
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = (String) authentication.getCredentials();

                User user = userService.findUserByName(username);
                if(user == null){
                    throw new UsernameNotFoundException("账号不存在");
                }

                password = CommunityUtil.md5(password+user.getSalt());
                if(!user.getPassword().equals(password)){
                    throw new BadCredentialsException("密码不正确");
                }
                //认证的主要信息 + 证书 + 权限
                return new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
            }
            //返回当前接口是哪种类型，账号密码？指纹？短信？
            @Override
            public boolean supports(Class<?> aClass) {
                //UsernamePasswordAuthenticationToken是authentication的常用的实现类，表示账号密码认证模式
                return UsernamePasswordAuthenticationToken.class.equals(aClass);
            }
        });
    }

    @Override
    //认证
    protected void configure(HttpSecurity http) throws Exception {
        //配置登录相关的配置，告诉那个请求是登录
        http.formLogin()
                .loginPage("/loginpage")
                .loginProcessingUrl("/login")
                .successHandler(new AuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
                        httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/index");
                    }
                })
                .failureHandler(new AuthenticationFailureHandler() {
                    @Override
                    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
                        //不能重定向,要用转发
                        //重定向：适合两个独立组件，当组件A完成没有什么东西返回时，（302）重定向到B，此时A不能给B带数据
                        //转发：两个独立组件，A只能完成请求的一半，然后由B处理，但是此时浏览器不知道B的存在（A和B有耦合）
                        //此处：A:login --> B:loginpage，也可以用模板，但是此处不行，不再controller内
                        httpServletRequest.setAttribute("error", e.getMessage());
                        httpServletRequest.getRequestDispatcher("/loginpage").forward(httpServletRequest, httpServletResponse);
                    }
                });
        http.logout()
                .logoutUrl("/logout")
                .logoutSuccessHandler(new LogoutSuccessHandler() {
                    @Override
                    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException, ServletException {
                        httpServletResponse.sendRedirect(httpServletRequest.getContextPath() + "/index");
                    }
                });

        //配置授权
        http.authorizeRequests()
                .antMatchers("/letter").hasAnyAuthority("USER", "ADMIN")
                .antMatchers("/admin").hasAnyAuthority("ADMIN")
                .and().exceptionHandling().accessDeniedPage("/denied");

        //增加filter
        http.addFilterBefore(new Filter(){
            @Override
            public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
                HttpServletRequest request = (HttpServletRequest) servletRequest;
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                if(request.getServletPath().equals("/login")){
                    String verifyCode = request.getParameter("verifyCode");
                    if(verifyCode == null || !verifyCode.equalsIgnoreCase("1234")){
                        request.setAttribute("error","验证码错误");
                        request.getRequestDispatcher("/loginpage").forward(request, response);
                        return;
                    }
                }
                //放行请求，请求继续执行
                filterChain.doFilter(request,response);
            }
        },UsernamePasswordAuthenticationFilter.class);

        http.rememberMe()
                .tokenRepository(new InMemoryTokenRepositoryImpl())
                .tokenValiditySeconds(3600 * 24)
                .userDetailsService(userService);
    }
}
```

#### 修改页面模板

在Security中，logout功能必须是post请求

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>首页</title>
</head>
<body>

    <h1>社区首页</h1>
    <!--欢迎信息-->
    <p th:if="${loginUser!=null}">
        欢迎你，<span th:text="${loginUser.username}"></span>
    </p>

    <ul>
        <li><a th:href="@{/discuss}">帖子详情</a></li>
        <li><a th:href="@{/letter}">私信列表</a></li>
        <li><a th:href="@{/loginpage}">登录</a></li>
        <!--<li><a th:href="@{/loginpage}">退出</a></li>-->
        <!--
        *Security实现的logout必须是post
        -->
        <li>
            <form method="post" th:action="@{/logout}">
                <a href="javascript:document.forms[0].submit();">退出</a>
            </form>
        </li>
    </ul>

</body>
</html>

```

## 权限控制

* 废弃拦截器，使用SpringSecurity来进行登录检查
* 授权配置，分配权限：普通用户，版主，管理员
* 使用原有认证方案：登录退出使用原来的代码
* CSRF配置：防止CSRF攻击

### 废弃拦截器

在WebMvcConfig中，讲login拦截器注释掉

### 设置权限

对一些路径进行权限设置/

```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {
    @Override
    //忽略静态资源
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    //授权
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/comment/add/**",
                        "/discuss/add",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "follow",
                        "unfollow"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .anyRequest().permitAll()
                .and().csrf().disable();
        //没有权限的异常处理
        http.exceptionHandling()
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    //权限不足处理
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if (xRequestedWith.equals("XMLHttpRequest")) {
                            response.setContentType("application/plain; charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "您无权限访问此功能"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                })
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    //没有登录处理
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String xRequestedWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestedWith)) {
                            response.setContentType("application/plain; charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403, "您还没有登录，请登录"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                });
        //默认Logout退出，会用filter拦截
        //将security中的默认登出路径设置如下，避免覆盖我们自己的登出代码
        http.logout().logoutUrl("/securityLogout");
        //Security获得权限需要在SecurityContext里获得
    }
}
```

在LoginTicket拦截器中，对登录的用户配置权限（因为在没有用security处理登录时，SecurityContext没有用户的权限信息，需要我们手动添加）

```java
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //从cookie中获取ticket
        String ticket = CookieUtil.getValue(request, "ticket");
        if (ticket != null) {
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                User user = userService.findUserById(loginTicket.getUserId());
                //在本此请求中持有用户，考虑多线程并发，多线程隔离
                hostHolder.setUser(user);
                //构建用户认证结果，并存入SecurityContext,以便于Security进行权限管理
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId())
                );
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }
```



### CSRF原理

某一网站盗取Cookie中的凭证（ticket），从而模拟用户提交表单。

解决办法：引入Security后，用户请求提交表单时，Security会在表单中加一个隐藏的Token，其他网站能够窃取到你的Ticket，但是无法获得你的表单数据（其中的Token），从而会被服务器发现。

问题：异步请求的时候需要自己处理，演示如下：

```java
<!--<meta name="_csrf" th:content="${_csrf.token}">-->
<!--<meta name="_csrf_header" th:content="${_csrf.headerName}">-->
$(function(){
	$("#publishBtn").click(publish);
});


function publish() {
	$("#publishModal").modal("hide");
	//发送AJAX请求前，需要带上CSRF令牌
//    var token = $("mata[name='_csrf']").attr("content");
//    var header = $("mata[name='_csrf_header']").attr("content");
//    $(document).ajaxSend(function(e, xhr, options){
//        xhr.setRequestHeader(header, token);
//    });
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

## 置顶，加精，删除

* 功能实现：置顶--->修改帖子类型，加精，删除 ---> 修改帖子状态
* 权限管理：用security设置即可
* 按钮显示：thymeleaf可以支持Security，但是需要添加包

### 实现置顶，加精，删除

数据层

```java
int updateType(@Param("id") int id, @Param("type") int type);

int updateStatus(@Param("id") int id, @Param("status") int status);
```

服务层

```java
public int updateType(int id, int type) {
    return discussPostMapper.updateType(id, type);
}

public int updateStatus(int id, int status) {
    return discussPostMapper.updateStatus(id, status);
}
```

控制层

```java
//置顶
@RequestMapping(path = "/top", method = RequestMethod.POST)
@ResponseBody
public String setTop(int id) {
    discussPostService.updateType(id, 1);

    //帖子同步ES
    //触发发帖事件
    //触发发帖事件
    Event event = new Event()
        .setTopic(TOPIC_PUBLISH)
        .setUserId(hostHolder.getUser().getId())
        .setEntityType(ENTITY_TYPE_POST)
        .setEntityId(id);
    producer.fireEvent(event);
    return CommunityUtil.getJSONString(0);
}

//加精
@RequestMapping(path = "/wonderful", method = RequestMethod.POST)
@ResponseBody
public String setWonderful(int id) {
    discussPostService.updateStatus(id, 1);

    //帖子同步ES
    //触发发帖事件
    //触发发帖事件
    Event event = new Event()
        .setTopic(TOPIC_PUBLISH)
        .setUserId(hostHolder.getUser().getId())
        .setEntityType(ENTITY_TYPE_POST)
        .setEntityId(id);
    producer.fireEvent(event);
    return CommunityUtil.getJSONString(0);
}

//删除
@RequestMapping(path = "/delete", method = RequestMethod.POST)
@ResponseBody
public String setDelete(int id) {
    discussPostService.updateStatus(id, 2);

    //帖子同步ES
    //触发发帖事件
    //触发删帖事件
    Event event = new Event()
        .setTopic(TOPIC_DELETE)
        .setUserId(hostHolder.getUser().getId())
        .setEntityType(ENTITY_TYPE_POST)
        .setEntityId(id);
    producer.fireEvent(event);
    return CommunityUtil.getJSONString(0);
}
```

### 设置权限

```java
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/comment/add/**",
                        "/discuss/add",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "follow",
                        "unfollow"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/delete"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                .anyRequest().permitAll()
                .and().csrf().disable();
```

## Redis高级数据类型

* HyperLogLog：超级日志
  * 基数算法，独立总数统计，多次请求去重
  * 数据占用空间少，只占12K的内存空间
  * 不精确的统计算法，标准误差为0.81%
* Bitmap：位图
  * 特殊格式字符串，按位存储数据
  * 其实是byte数组
  * 大量连续数据的布尔值，如每天的签到。如一年的签到数据：365/8，只有40多个字节

都适合用来给网站运营的数据进行统计，并且节约内存

使用redis中的hyperloglog和bitmap，代码如下：

```java
    //统计20W个数据的独立整合
    @Test
    public void testHyperLogLog() {
        String redisKey = "test:hll:01";
        for (int i = 1; i <= 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }

        for (int i = 1; i <= 100000; i++) {
            int r = (int) (Math.random() * 10000 + 1);
            redisTemplate.opsForHyperLogLog().add(redisKey, r);
        }
        long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);
    }

    //将三组数据合并，在合并后的重复数据中，统计独立正数
    @Test
    public void testHyperLogLogUnion() {
        String redisKey2 = "test:hll:02";
        for (int i = 1; i <= 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }
        String redisKey3 = "test:hll:03";
        for (int i = 5001; i <= 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }

        String redisKey4 = "test:hll:04";
        for (int i = 10001; i <= 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey4, i);
        }
        String unionKey = "test:hll:union";
        redisTemplate.opsForHyperLogLog().union(unionKey, redisKey2, redisKey3, redisKey4);

        long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size);
    }

    //统计一组数据的布尔值
    @Test
    public void testBitMap() {
        String redisKey = "test:bm:01";

        //记录
        redisTemplate.opsForValue().setBit(redisKey, 1, true);
        redisTemplate.opsForValue().setBit(redisKey, 4, true);
        redisTemplate.opsForValue().setBit(redisKey, 7, true);

        //查询
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));

        //统计
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);
    }

    //统计三组数据布尔值，并进行or运算
    @Test
    public void testBitMapOperation() {
        String redisKey2 = "test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2, 0, true);
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);
        redisTemplate.opsForValue().setBit(redisKey2, 2, false);

        String redisKey3 = "test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey3, 2, true);
        redisTemplate.opsForValue().setBit(redisKey3, 3, true);
        redisTemplate.opsForValue().setBit(redisKey3, 4, false);

        String redisKey4 = "test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey4, 4, true);
        redisTemplate.opsForValue().setBit(redisKey4, 5, true);
        redisTemplate.opsForValue().setBit(redisKey4, 6, false);

        String redisKey = "test:bm:or";
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR, redisKey.getBytes(),
                        redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);
    }
}
```

## 网站数据统计

* UV：独立访客，通过IP地址统计，希望把匿名用户（游客）也统计起来。每次访问都要统计，因为不确定此次访问的IP是否已经记录过。用HyperLogLog进行统计。
* DAU：日活跃用户，访问过一次则认为其是活跃或用。与UV的区别是：通过userId进行排重（已注册用户），而且是比较精确的结果，故使用bitmap。可以用UserId为Index,在redis中存储，（key为日期），所以每天需要大概几十万位。

### 定义RedisKey

分别定义UA和DAU的rediskey

```java
    //单日UV
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    //区间UV
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    //单日DAU
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    //区间DAU
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }
```

### DataService

```java
@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    //记录UV数据，记录指定IP
    public void recordIV(String ip) {
        String redisKey = RedisKeyUtil.getUVKey(dateFormat.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);
    }

    //统计指定范围内的UV
    public long calculateUV(Date start, Date end) {
        if (end == null || start == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        //整理key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getUVKey(dateFormat.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE, 1);
        }
        String redisKey = RedisKeyUtil.getUVKey(dateFormat.format(start), dateFormat.format(end));
        redisTemplate.opsForHyperLogLog().union(redisKey, keyList.toArray());
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    //记录指定用户到DAU
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(dateFormat.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);
    }

    //统计指定范围内的DAU
    public long calculateDAU(Date start, Date end) {
        if (end == null || start == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        //整理key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getDAUKey(dateFormat.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE, 1);
        }
        String redisKey = RedisKeyUtil.getDAUKey(dateFormat.format(start), dateFormat.format(end));
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(dateFormat.format(start), dateFormat.format(end));
                connection.bitOp(RedisStringCommands.BitOperation.OR, redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });
    }
}
```

### 拦截器

每次请求需要判断Ip和用户

```java
@Component
public class Datainteceptor implements HandlerInterceptor {
    @Autowired
    private DataService dataService;
    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //记录UV
        String ip = request.getRemoteHost();
        dataService.recordIV(ip);

        //记录DAU
        User user = hostHolder.getUser();
        if (user != null) {
            dataService.recordDAU(user.getId());
        }
        return true;
    }
}
```

### Controller

实现访问数据页/查询UV/查询dau的功能

```java
@Controller
public class DataController {
    @Autowired
    private DataService dataService;

    @RequestMapping(path = "/data", method = {RequestMethod.GET, RequestMethod.POST})
    public String getDataPage() {
        return "/site/admin/data";
    }

    @RequestMapping(path = "/data/uv", method = RequestMethod.POST)
    public String getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                        @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult", uv);
        model.addAttribute("uvStartDate", start);
        model.addAttribute("uvEndDate", end);
        return "forward:/data";
    }

    @RequestMapping(path = "/data/dau", method = RequestMethod.POST)
    public String getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date start,
                         @DateTimeFormat(pattern = "yyyy-MM-dd") Date end, Model model) {
        long uv = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult", uv);
        model.addAttribute("dauStartDate", start);
        model.addAttribute("dauEndDate", end);
        return "forward:/data";
    }
}
```

### 配置权限

在SecurityConfig下添加路径即可

## 任务执行和调度

* JDK线程池
  * ExecutorService
  * ScheduledExecutorService
* Spring线程池
  * ThreadPoolTaskExecutor
  * ThreadPoolTaskScheduler
* 分布式定时任务
  * Spring Quartz

在分布式部署中，使用JDK或者Spring的线程池调度，会有问题。在分布式服务器中，请求会通过Nginx来分配，从而可以实现负载均衡，但是Scheduler不能被Nigix管理，所以同一的Scheduler请求，可能会在多服务器执行多次。

使用Quartz，不同服务器的Scheduler运行时，会在数据库服务器访问运行数据，通过排队（加锁），可以使得程序只运行一次。

### JDK线程池演示

```java
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ThreadPoolTest {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTest.class);

    //演示JDK普通线程池
    //包含5个线程
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    //定时执行任务的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    //使用test方法，让当前线程sleep，以避免线程自动结束
    private void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //演示JDK普通线程池
    @Test
    public void testExecutorService() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello, executorService");
            }
        };
        for (int i = 0; i < 10; i++) {
            executorService.submit(task);
        }
        sleep(10000);
    }

    //JDK定时任务线程池
    @Test
    public void testScheduledExecutorService() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello, scheduledExecutorService");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(task, 10000, 1000, TimeUnit.MILLISECONDS);
        sleep(30000);
    }
}
```

### Spring线程池

配置：

```
#TaskExecutionProperties
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=15
spring.task.execution.pool.queue-capacity=100
#TaskSchedulingProperties
spring.task.scheduling.pool.size=5
```

演示：

```java
    //演示Spring普通线程池
    @Test
    public void testThreadPoolTaskExecutor() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello, ThreadPoolTaskExecutor");
            }
        };
        for (int i = 0; i < 10; i++)
            taskExecutor.submit(task);
        sleep(10000);
    }

    //演示Spring定时线程
    @Test
    public void testThreadPoolTaskScheduler() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello, ThreadPoolTaskScheduler");
            }
        };
        Date startTime = new Date(System.currentTimeMillis() + 10000);
        taskScheduler.scheduleAtFixedRate(task, startTime, 1000);
        sleep(30000);
    }
```

### Spring使用多线程的简单方法

通过注解即可

```java
    @Async
    public void execute1() {
        System.out.println("当前执行线程为" + Thread.currentThread().getName());
    }

    @Scheduled(initialDelay = 10000, fixedDelay = 1000)
    public void scheduled1() {
        System.out.println("当前执行定时任务，执行线程为 --->" + Thread.currentThread().getName());
    }
```

```java
    //Spring多线程简便方法
    @Test
    public void testThreadPoolTaskExecutorSimple() {
        for (int i = 0; i < 10; i++)
            alphaService.execute1();
        sleep(10000);
    }

    //Spring定时任务简化
    @Test
    public void testThreadPoolTaskSchedulerSimple() {
        sleep(30000);
    }
```

### Quartz演示

需要提前创建Database，其中存放运行定时任务，所需要的信息

默认读取内存中数据，所以需要配置

```
# QuartzProperties
spring.quartz.job-store-type=jdbc
spring.quartz.scheduler-name=communityScheduler
spring.quartz.properties.org.quartz.scheduler.instanceId=AUTO
spring.quartz.properties.org.quartz.jobStore.class=org.quartz.impl.jdbcjobstore.JobStoreTX
spring.quartz.properties.org.quartz.jobStore.driverDelegateClass=org.quartz.impl.jdbcjobstore.StdJDBCDelegate
spring.quartz.properties.org.quartz.jobStore.isClustered=true
spring.quartz.properties.org.quartz.threadPool.class=org.quartz.simpl.SimpleThreadPool
spring.quartz.properties.org.quartz.threadPool.threadCount=5
```

定义QuarzJob

```java
public class AlphaJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println(Thread.currentThread().getName() + ": execute quartz job");
    }
}
```

配置Quartz

```java
@Configuration
public class QuartzConfig {
    //FactoryBean，可以简化Bean的实例化过程
    //1.通过factoryBean封装Bean的实例化过程
    //2.将FactorBean装配到容器里
    //3.FactoryBean注入给其他的Bean，
    //4.其他的Bean得到了FactoryBean所管理的实例对象

    @Bean
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class);
        factoryBean.setName("alphaJob");
        factoryBean.setGroup("alphaGroup");
        //长久保存
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean simpleTrigger(JobDetail alphaJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail);
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }
}
```

此时上述Job就能在项目启动后，自动运行，如果需要删除，可执行如下代码

```java
public class QuartzTest {
    @Autowired
    private Scheduler scheduler;

    @Test
    public void testDeleteJob() {
        try {
            boolean result = scheduler.deleteJob(new JobKey("alphaJob", "alphaGroup"));
            System.out.println(result);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
```

## 热帖排名

时间增加，分数减小，评论/收藏/点赞越多，分数越高。

分数计算方式：定时算一次，从而能保证帖子的分数一段时间是不变的。

只算分数变化的帖子 ---> 把分数变化的帖子放在Redis中。

### 统计帖子分数

#### RedisKey

```java
public static String getPostScore() {
    return PREFIX_POSt + SPLIT + "socre";
}
```

#### 能修改帖子分数的地方，存入Redis

包括点赞/评论/发帖/加精

#### 定义定时任务

每隔5分钟运行一次

```java
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreReferenceJob.class);
        factoryBean.setName("postScoreReferenceJob");
        factoryBean.setGroup("communityJobGroup");
        //长久保存
        factoryBean.setDurability(true);
        factoryBean.setRequestsRecovery(true);
        return factoryBean;
    }

    //刷新帖子分数
    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("communityTriggerGroup");
        factoryBean.setRepeatInterval(1000 * 60 * 5);
        factoryBean.setJobDataMap(new JobDataMap());
        return factoryBean;
    }
```

计算分数（job)

```java
public class PostScoreReferenceJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreReferenceJob.class);
    //牛客纪元
    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客网纪元失败" + e);
        }
    }

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private ElasticsearchService elasticsearchService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScore();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);
        if (operations.size() == 0) {
            logger.info("任务取消，没有需要重新计分的帖子");
        }
        logger.info("任务开始，正在计算帖子分数： " + operations.size());
        while (operations.size() > 0) {
            this.refresh((Integer) operations.pop());
        }
        logger.info("帖子分数计算结束！");
    }

    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null) {
            logger.error("帖子不存在，postId :" + postId);
            return;
        }
        //计算是否加精，评论数，点赞数
        boolean wonderful = post.getStatus() == 1;
        int commentCount = post.getCommentCount();
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
        //计算权重
        double w = (wonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        double score = Math.log10(Math.max(w, 1))
                + (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        //更新帖子的分数
        discussPostService.updateScore(postId, score);
        //同步service
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }
}
```

### 修改排序方式

#### 数据访问层

```java
@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPosts(@Param("userId") int userId, @Param("offset") int offset, @Param("limit") int limit, @Param("orderMode") int orderMode);
```

#### 服务层

```java
public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }
```

#### 控制层

orderMode通过路径传过来

```java
    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page, @RequestParam(name = "orderMode", defaultValue = "0") int orderMode) {
        page.setRows(discussPostService.findDiscussPostsRows(0));
        page.setPath("/index?orderMode=" + orderMode);
        //在方法调用前，SpringMVC会自动实例化Page和Model，并且将Page注入Model
        //所以可以在thymeleaf中直接访问Page对象中的数据。
        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(), orderMode);
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        for (DiscussPost post : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("post", post);
            User user = userService.findUserById(post.getUserId());
            map.put("user", user);
            long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
            map.put("likeCount", likeCount);
            discussPosts.add(map);
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("orderMode", orderMode);
        return "/index";
    }
```

Idea查看方法调用的快捷键：alt + F7

## 生成长图

* 将模板文件转换为PDF（在服务端）
* 使用wkhtmltopdf
  * 1，wkhtmltopdf url file
  * 2，wkhtmltoimage url file

* 通过java
  * Runtime.getRuntime().exec();
* [官网](https://wkhtmltopdf.org/)
* 配置环境变量

通过命令访问工具

```
wkhtmltoimage --quality 75 https://www.nowcoder.com d:/Java/nowcoder_project/wk_image/1.png
```

### 模拟分享功能

将命令行设置为可配置，将图片位置设置为可配，检查路径是否存在（wk不能生成路径）

创建配置文件：

```
#wk
wk.image.command=d:/Java/nowcoder_project/wkhtmltopdf/bin/wkhtmltoimage
wk.image.storage=d:/Java/nowcoder_project/wk_image
```

#### ShareController

使用Kafka，并且异步驱动，可以节省时间

```java
@Controller
public class ShareController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private EventProducer eventProducer;

    @Value("${community.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Value("${wk.image.storage}")
    private String wkImageStorage;

    //生成图片：异步驱动
    //处理为事件，因为生成的时间可能比较长，用户不需要在页面等待。
    @RequestMapping(path = "/share", method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl) {
        //文件名随机
        String fileName = CommunityUtil.generateUUID();
        Event event = new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl", htmlUrl)
                .setData("fileName", fileName)
                .setData("suffix", ".png");
        eventProducer.fireEvent(event);
        //返回访问路径
        Map<String, Object> map = new HashMap<>();
        map.put("shareUrl", domain + contextPath + "/share/image/" + fileName);
        return CommunityUtil.getJSONString(0, null, map);
    }

    //获取长图
    @RequestMapping(path = "/share/image/{fileName}", method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        response.setContentType("image/png");
        File file = new File(wkImageStorage + "/" + fileName + ".png");
        try {
            OutputStream os = response.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int b = 0;
            while (b != -1) {
                b = fis.read(buffer);
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("获取长图失败" + e.getMessage());
        }
    }
}
```

#### EventConsumer

处理分享事件

```java
    //消费分享时间
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息内容为空");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息格式错误");
            return;
        }
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");

        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成图片成功" + cmd);
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("生成图片失败" + e.getMessage() + "  " + cmd);
        }
    }
```

## 文件上传云服务器

* 客户端上传
* 服务器直传

需要注册[七牛云](https://www.qiniu.com/)，并且创建两个空间，用于存储用户头像和用户share图片

设置七牛云的配置参数

```
#qiniu
qiniu.key.access=LeG24jUYDhS6YFSxe2d6EEC1rksz3MmZ0yeYu7F3
qiniu.key.secret=H-BUVFxvB1QoqKzv6abOCMT_NEzAPvz3pPodJo3Q
qiniu.bucket.header.name=zt-community-header
qiniu.bucket.header.url=qbbxkqk6q.bkt.clouddn.com
qiniu.bucket.share.name=zt-community-share
qiniu.bucket.share.url=qbbx267e5.bkt.clouddn.com
```

### 从客户端分享到云服务器

在进入设置页面后，为此次设置（上传图片），生成一个uploadToken，和fileName，传给页面。

```java
    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        //生成上传文件名称
        String fileName = CommunityUtil.generateUUID();
        //设置响应信息
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));
        //生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);
        return "/site/setting";
    }
```

页面根据得到的token和filename，异步的使用post命令，将文件上传到云服务器

```js
$(function(){
    $("#uploadForm").submit(upload);
});

function upload() {
    $.ajax({
        url: "http://upload-z2.qiniup.com",
        method: "post",
        processData: false,
        contentType: false,
        data: new FormData($("#uploadForm")[0]),
        success: function(data) {
            if(data && data.code == 0) {
                // 更新头像访问路径
                $.post(
                    CONTEXT_PATH + "/user/header/url",
                    {"fileName":$("input[name='key']").val()},
                    function(data) {
                        data = $.parseJSON(data);
                        if(data.code == 0) {
                            window.location.reload();
                        } else {
                            alert(data.msg);
                        }
                    }
                );
            } else {
                alert("上传失败!");
            }
        }
    });
    return false;
}
```

由上面js代码可以，如果上传成功，则访问更改用户头像的路径来更改header_url。

```java
//User表中的headerUrl需要更新
@RequestMapping(path = "/header/url", method = RequestMethod.POST)
@ResponseBody
public String updateHeaderUrl(String fileName) {
    if (StringUtils.isBlank(fileName)) {
        return CommunityUtil.getJSONString(1, "文件名不能为空");
    }
    String url = "http://" + headerBucketUrl + "/" + fileName;
    userService.updataHeader(hostHolder.getUser().getId(), url);
    return CommunityUtil.getJSONString(0);
}
```

### 从服务器分享到云服务器