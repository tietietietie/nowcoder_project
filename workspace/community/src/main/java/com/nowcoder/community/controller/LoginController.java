package com.nowcoder.community.controller;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "/site/register";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    //会自动注入给user对象
    public String register(Model model, User user) {
        Map<String, Object> map = userService.register(user);
        if (map.isEmpty()) {
            model.addAttribute("msg", "注册成功，激活邮件已发送！请查阅。");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }
    }
}
