package com.coderpwh.demo.service;

import com.coderpwh.mvc.v1.annotation.MyService;

/**
 * @author coderpwh
 * @date 2022/5/2 11:11 AM
 */
@MyService
public class DemoService {
    public String sayHello(String name) {
        return "hello " + name;
    }
}
