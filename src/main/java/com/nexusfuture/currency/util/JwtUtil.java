package com.nexusfuture.currency.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {
    
    private final SecretKey SECRET_KEY;
    private final Duration ACCESS_TTL;
    private final Duration REFRESH_TTL;

    public JwtUtil(@Value("${jwt.secret:CurrencyServiceDefaultSecretKey_2026_LongString_32Chars}") String secret,
                   @Value("${jwt.access-token-ttl:30m}") String accessTtl,
                   @Value("${jwt.refresh-token-ttl:7d}") String refreshTtl) {
        
        // 1. 初始化密钥
        this.SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        
        // 2. 安全解析时间配置
        this.ACCESS_TTL = parseDurationSafe(accessTtl, Duration.ofMinutes(30));
        this.REFRESH_TTL = parseDurationSafe(refreshTtl, Duration.ofDays(7));
    }

    private Duration parseDurationSafe(String val, Duration defaultDuration) {
        try {
            String formatted = val.toUpperCase();
            if (formatted.endsWith("M")) formatted = "PT" + formatted;
            else if (formatted.endsWith("D")) formatted = "P" + formatted;
            else if (formatted.endsWith("H")) formatted = "PT" + formatted;
            return Duration.parse(formatted);
        } catch (Exception e) {
            log.warn("⚠️ JWT 时间配置解析失败: {}, 使用默认值: {}", val, defaultDuration);
            return defaultDuration;
        }
    }

    public String generateAccessToken(String userId, String username) {
        return buildToken(userId, username, ACCESS_TTL, "ACCESS");
    }

    public String generateRefreshToken(String userId, String username) {
        return buildToken(userId, username, REFRESH_TTL, "REFRESH");
    }

    private String buildToken(String userId, String username, Duration ttl, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", type);
        
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttl.toMillis());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SECRET_KEY)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUserId(String token) {
        return extractClaims(token).get("userId", String.class);
    }

    public String extractType(String token) {
        return extractClaims(token).get("type", String.class);
    }
}
