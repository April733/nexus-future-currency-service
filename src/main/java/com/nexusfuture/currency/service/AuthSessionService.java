package com.nexusfuture.currency.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${auth.session.ttl:7d}")
    private String sessionTtlStr;

    private static final String SESSION_PREFIX = "auth:session:";

    /**
     * 创建会话：将 RefreshToken 存入 Redis
     */
    public void createSession(String userId, String refreshToken) {
        long ttlSeconds = parseTtlToSeconds(sessionTtlStr);
        String key = SESSION_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, ttlSeconds, TimeUnit.SECONDS);
        log.info("用户 [{}] 会话已创建，TTL: {} 秒", userId, ttlSeconds);
    }

    /**
     * 校验会话：检查 Redis 中是否存在该用户的 RefreshToken 且匹配
     */
    public boolean validateSession(String userId, String refreshToken) {
        String key = SESSION_PREFIX + userId;
        Object storedToken = redisTemplate.opsForValue().get(key);
        return storedToken != null && storedToken.equals(refreshToken);
    }

    /**
     * 删除会话：用于登出或 Token 轮换
     */
    public void deleteSession(String userId) {
        String key = SESSION_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("用户 [{}] 会话已销毁", userId);
    }

    /**
     * 撤销会话：等同于 deleteSession，用于语义化调用
     */
    public void revokeSession(String userId) {
        deleteSession(userId);
    }

    /**
     * 轮换会话：删除旧会话并创建新会话（用于 Refresh Token 刷新时）
     */
    public void rotateSession(String userId, String newRefreshToken) {
        deleteSession(userId); // 先销毁旧的
        createSession(userId, newRefreshToken); // 再创建新的
    }

    /**
     * 获取存储的 RefreshToken
     */
    public String getStoredRefreshToken(String userId) {
        Object token = redisTemplate.opsForValue().get(SESSION_PREFIX + userId);
        return token != null ? token.toString() : null;
    }

    /**
     * 安全解析 TTL 字符串 (支持 7d, 30m, 1h 等格式)
     */
    private long parseTtlToSeconds(String ttlStr) {
        if (ttlStr == null || ttlStr.isEmpty()) {
            return 7 * 24 * 3600; // 默认 7 天
        }
        
        try {
            // 尝试直接解析 ISO 格式 (如 PT168H)
            return Duration.parse(ttlStr).getSeconds();
        } catch (Exception e) {
            // 处理简写格式 (如 7d, 12h, 30m)
            if (ttlStr.endsWith("d")) {
                return Long.parseLong(ttlStr.replace("d", "")) * 24 * 3600;
            } else if (ttlStr.endsWith("h")) {
                return Long.parseLong(ttlStr.replace("h", "")) * 3600;
            } else if (ttlStr.endsWith("m")) {
                return Long.parseLong(ttlStr.replace("m", "")) * 60;
            } else {
                return Long.parseLong(ttlStr); // 假设是纯数字秒数
            }
        }
    }
}
