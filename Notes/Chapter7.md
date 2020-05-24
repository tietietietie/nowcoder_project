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

