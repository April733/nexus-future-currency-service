package com.nexusfuture.currency.controller;

import com.nexusfuture.currency.common.Result;
import com.nexusfuture.currency.entity.User;
import com.nexusfuture.currency.service.AuthSessionService;
import com.nexusfuture.currency.service.UserService;
import com.nexusfuture.currency.util.JwtUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthSessionService sessionService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(
            @NotBlank(message = "用户名不能为空") @RequestParam String username, 
            @NotBlank(message = "密码不能为空") @RequestParam String password) {
        
        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty() || !userService.checkPassword(userOpt.get(), password)) {
            return Result.fail(401, "用户名或密码错误");
        }

        User user = userOpt.get();
        String accessToken = jwtUtil.generateAccessToken(user.getUserId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserId(), user.getUsername());
        
        // 持久化会话到 Redis
        sessionService.createSession(user.getUserId(), refreshToken);
        
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        return Result.success(result);
    }

    @PostMapping("/refresh")
    public Result<Map<String, String>> refreshToken(@RequestParam String refreshToken) {
        String userId = jwtUtil.extractUserId(refreshToken);
        
        // 校验 Redis 中的会话是否有效
        if (!sessionService.validateSession(userId, refreshToken)) {
            return Result.fail(401, "Refresh Token 无效或已过期");
        }

        // 签发新对 (Token Rotation)
        User user = userService.getUserByUserId(userId).orElseThrow();
        String newAccess = jwtUtil.generateAccessToken(user.getUserId(), user.getUsername());
        String newRefresh = jwtUtil.generateRefreshToken(user.getUserId(), user.getUsername());
        
        // 更新 Redis 会话
        sessionService.rotateSession(userId, newRefresh);
        
        Map<String, String> result = new HashMap<>();
        result.put("accessToken", newAccess);
        result.put("refreshToken", newRefresh);
        return Result.success(result);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.extractUserId(token);
        sessionService.revokeSession(userId);
        return Result.success(null);
    }
}