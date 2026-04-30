package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.entity.User;
import com.nexusfuture.currency.service.UserService;
import com.nexusfuture.currency.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

// ✅ 修正：Spring Boot 3.x 使用 jakarta 包
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户管理控制器
 * 提供用户注册、登录、查询等基础功能
 * 
 * @author april
 * @since 2026-04-29
 */
@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 用户注册接口（公开）
     * 
     * @param username 用户名（必填，3-20字符，必须唯一）
     * @param email 邮箱地址（必填，需符合邮箱格式，必须唯一）
     * @param password 密码（必填，6-50字符）
     * @return 注册成功提示或错误信息
     */
    @PostMapping("/register")
    public Result<String> register(
            @NotBlank(message = "用户名不能为空") 
            @Size(min = 3, max = 20, message = "用户名长度必须在 3-20 之间") 
            @RequestParam String username, 
            
            @NotBlank(message = "邮箱不能为空") 
            @Email(message = "邮箱格式不正确") 
            @RequestParam String email, 
            
            @NotBlank(message = "密码不能为空") 
            @Size(min = 6, max = 50, message = "密码长度必须在 6-50 之间") 
            @RequestParam String password) {
        
        try {
            userService.registerUser(username, email, password);
            return Result.success("注册成功");
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 用户登录接口（公开）
     * 验证用户名和密码，成功则返回 JWT Token
     * 
     * @param username 用户名（必填）
     * @param password 密码（必填）
     * @return 包含 JWT Token 的结果对象或错误提示
     */
    @PostMapping("/login")
    public Result<Map<String, String>> login(
            @NotBlank(message = "用户名不能为空") 
            @RequestParam String username, 
            
            @NotBlank(message = "密码不能为空") 
            @RequestParam String password) {
        
        // 1. 尝试查找用户
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return Result.fail(400, "用户不存在，请先注册");
        }

        // 2. 验证密码
        if (!userService.checkPassword(userOpt.get(), password)) {
            return Result.fail(401, "密码错误，请重试");
        }

        // 3. 生成 Token
        User user = userOpt.get();
        String token = jwtUtil.generateToken(user.getUsername());
        
        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        return Result.success(result);
    }

    /**
     * 根据 ID 查询用户（需要认证）
     * 
     * 请求示例：
     * curl -X GET 'http://localhost:8080/api/users/1' \
     *   -H 'Authorization: Bearer YOUR_JWT_TOKEN_HERE'
     * 
     * @param id 用户 ID（必填）
     * @return 用户实体或错误提示
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(user -> Result.success(user))
                .orElse(Result.fail(404, "用户不存在，请检查用户ID是否正确"));
    }
}