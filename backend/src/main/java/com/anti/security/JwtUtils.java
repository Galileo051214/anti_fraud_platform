package com.anti.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role, Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("userId", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("role", String.class);
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        // 兼容 JJWT 将数字 claim 解析成 Integer/Long 的情况
        Object rawUserId = claims.get("userId");
        if (rawUserId == null) return null;
        if (rawUserId instanceof Number) return ((Number) rawUserId).longValue();
        try {
            return Long.parseLong(String.valueOf(rawUserId));
        } catch (Exception e) {
            return null;
        }
    }

    public Long getExpirationFromToken(String token) {
        Claims claims = parseToken(token);
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return 0L;
        }
        return expiration.getTime() - System.currentTimeMillis();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT格式错误: {}", e.getMessage());
        } catch (SecurityException e) {
            log.error("JWT签名失败: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT为空: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
