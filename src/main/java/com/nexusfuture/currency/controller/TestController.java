package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    // GET 接口
    @GetMapping("/hello")
    public Result<Map<String, String>> hello() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "AI Infra");
        map.put("desc", "连接能力工程师");
        return Result.success(map);
    }

    // POST 接口
    @PostMapping("/login")
    public Result<String> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }

        return Result.success("登录成功，用户：" + username);
    }
}