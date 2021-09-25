package com.lee.jason.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author huanli9
 * @description
 * @date 2021/9/6 22:56
 */
@Controller
public class TestController {

    @GetMapping("/test")
    public @ResponseBody String test() {
        return "test";
    }
}
