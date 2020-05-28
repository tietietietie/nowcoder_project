package com.nowcoder.community.config;

import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

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
        http.logout().logoutUrl("/securityLogout");
        //Security获得权限需要在SecurityContext里获得
    }
}
