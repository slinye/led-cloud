package com.led.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/** JWT 工具类 */
@Slf4j
public class JwtUtil {

    private static final String SECRET = "led-cloud-microservice-jwt-secret-key-2024-min-256bits!!";
    private static final long EXPIRATION = 24 * 60 * 60 * 1000L; // 24小时
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    /** 生成Token */
    public static String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 解析Token */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT已过期: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /** 验证Token是否有效 */
    public static boolean validateToken(String token) {
        return parseToken(token) != null;
    }

    /** 从Token获取用户名 */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    /** 从Token获取用户ID */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims != null) {
            String sub = claims.getSubject();
            return sub != null ? Long.valueOf(sub) : null;
        }
        return null;
    }

    /** 从Token获取角色 */
    public static String getRole(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get("role", String.class) : null;
    }
}
