package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page) {
        page.setRows(discussPostService.findDiscussPostsRows(0));
        page.setPath("/index");
        //在方法调用前，SpringMVC会自动实例化Page和Model，并且将Page注入Model
        //所以可以在thymeleaf中直接访问Page对象中的数据。
        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffSet(), page.getLimit());
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        for (DiscussPost post : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("post", post);
            User user = userService.findUserById(post.getUserId());
            map.put("user", user);
            discussPosts.add(map);
        }
        model.addAttribute("discussPosts", discussPosts);
        return "/index";
    }
}