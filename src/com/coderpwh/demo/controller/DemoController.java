package com.coderpwh.demo.controller;

import com.coderpwh.mvc.v1.annotation.MyAutowired;
import com.coderpwh.mvc.v1.annotation.MyController;
import com.coderpwh.mvc.v1.annotation.MyRequestMapping;
import com.coderpwh.mvc.v1.annotation.MyRequestParam;
import com.coderpwh.demo.service.DemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author coderpwh
 * @date 2022/5/2 11:13 AM
 */
@MyController
@MyRequestMapping("/demo")
public class DemoController {
    @MyAutowired
    private DemoService demoService;
    @MyRequestMapping("/query")
    public void sayHello(HttpServletRequest req, HttpServletResponse res, @MyRequestParam("name") String name) {
        String s = demoService.sayHello(name);
        try {
            res.getWriter().write(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @MyRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse res, @MyRequestParam("a") Integer a,@MyRequestParam("b") Integer b) {
        try {
            res.getWriter().write("a + b = "+a+b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @MyRequestMapping("/test")
    public void test(HttpServletRequest req,HttpServletResponse response) {
        try {
            response.getWriter().write("test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
