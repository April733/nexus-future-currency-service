package com.nexusfuture.currency.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtUtil {
    
    private final SecretKey SECRET_KEY;

    public JwtUtil(@Value("${jwt.secret:defaultSecretKeyForCurrencyService2026}") String secret) {
        if (secret.length() < 32) {
            log.warn("JWT 密钥长度不足 32 位，使用默认密钥（生产环境请配置更长的密钥）");
            this.SECRET_KEY = Keys.hmacShaKeyFor("defaultSecretKeyForCurrencyService2026MustBe32Chars".getBytes(StandardCharsets.UTF_8));
        } else {
            this.SECRET_KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    public String generateToken(String username, String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
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

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return extractClaims(token).get("userId", String.class);
    }

    public boolean validateToken(String token, String username) {
        try {
            Claims claims = extractClaims(token);
            return username.equals(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.warn("Token 已过期: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("不支持的 Token 格式: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("Token 格式错误: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            return false;
        }
    }
}
