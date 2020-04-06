package com.nowcoder.community.controller;

import com.nowcoder.community.service.AlphaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@Controller
@RequestMapping("/alpha")
public class AlphaController {

    @Autowired
    private AlphaService alphaService;

    @RequestMapping("/hello")
    @ResponseBody
    public String sayHello() {
        return "Hello, Spring Boot.";
    }

    @RequestMapping("/data")
    @ResponseBody
    public String getData() {
        return alphaService.find();
    }

    @RequestMapping("/http")
    public void http(HttpServletRequest request, HttpServletResponse response) {
        //输出请求信息（在服务器控制台）
        System.out.println(request.getMethod());
        System.out.println(request.getServletPath());
        Enumeration<String> enumeration = request.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String name = enumeration.nextElement();
            String value = request.getHeader(name);
            System.out.println(name + " : " + value);
        }

        //通过向response写数据，从而向服务器输出
        response.setContentType("text/html;charset=utf-8");
        try (PrintWriter writer = response.getWriter();) {
            writer.write("<h1>ZT的牛客网<h1>");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //GET请求（用于获取某些数据）（默认）
    //查询所有的学生，分页显示
    //只能处理get请求，指定
    //参数名和浏览器传过来的一致
    //非常容易获得参数，还可以加注解进行限定/明确
    @RequestMapping(path = "/students", method = RequestMethod.GET)
    @ResponseBody
    public String getStudents(
            @RequestParam(name = "current", required = false, defaultValue = "1") int current,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        System.out.println(current);
        System.out.println(limit);
        return "Some Students";
    }

    //student/123
    //参数可以称为路径的一部分
    @RequestMapping(path = "/student/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(@PathVariable("id") int id) {
        return "a student : " + id;
    }

    //post请求
    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(String name, int age) {
        System.out.println(name);
        System.out.println(age);
        return "success";
    }

    //响应html数据
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public ModelAndView gerTeacher() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("name", "张铁");
        modelAndView.addObject("age", "24");
        modelAndView.setViewName("/demo/view");
        return modelAndView;
    }

    //查询学校
    @RequestMapping(path = "/school", method = RequestMethod.GET)
    public String getSchool(Model model) {
        model.addAttribute("name", "HUST");
        model.addAttribute("age", 100);
        return "/demo/view";
    }

    //json(异步请求）
    @RequestMapping(path = "/emp", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getEmp() {
        Map<String, Object> emp = new HashMap<>();
        emp.put("name", "zhangtie");
        emp.put("age", 23);
        emp.put("salary", 20000);
        return emp;
    }

    @RequestMapping(path = "/emps", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String, Object>> getEmps() {
        Map<String, Object> emp = new HashMap<>();
        List<Map<String, Object>> list = new ArrayList<>();
        emp.put("name", "zhangtie");
        emp.put("age", 23);
        emp.put("salary", 20000);
        list.add(emp);
        emp = new HashMap<>();
        emp.put("name", "lisi");
        emp.put("age", "22");
        list.add(emp);
        emp = new HashMap<>();
        emp.put("name", "wangwu");
        emp.put("age", "23");
        list.add(emp);
        return list;
    }
}
