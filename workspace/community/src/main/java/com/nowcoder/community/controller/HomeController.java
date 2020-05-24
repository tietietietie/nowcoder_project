package com.nowcoder.community.controller;

import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
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
public class HomeController implements CommunityConstant {
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;

    @RequestMapping(path = "/index", method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page) {
        page.setRows(discussPostService.findDiscussPostsRows(0));
        page.setPath("/index");
        //在方法调用前，SpringMVC会自动实例化Page和Model，并且将Page注入Model
        //所以可以在thymeleaf中直接访问Page对象中的数据。
        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
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
        return "/index";
    }

    //跳转到500错误
    @RequestMapping(path = "/error", method = RequestMethod.GET)
    public String getErrorPage() {
        return "/error/500";
    }

    @RequestMapping(path = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "/error/404";
    }
}
