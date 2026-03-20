package xyz.kip.gateway.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 *
 * @author xiaoshichuan
 * @version 2026-02-28
 */
@RefreshScope
@Component
public class JwtUtil {

    @Value("${auth.jwt.secret:KipAuthServiceSecretKeyMustBeAtLeast32CharactersLongForHS256}")
    private String jwtSecret;

    @Value("${auth.jwt.expiration:86400000}")
    private Long jwtExpiration;

    /**
     * 生成JWT token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return token
     */
    public String generateToken(String userId, String username) {
        return generateToken(userId, username, new HashMap<>());
    }

    /**
     * 生成JWT token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param claims   额外声明
     * @return token
     */
    public String generateToken(String userId, String username, Map<String, Object> claims) {
        claims.put("userId", userId);
        claims.put("username", username);
        return createToken(claims, userId);
    }

    /**
     * 创建token
     *
     * @param claims  声明
     * @param subject 主题
     * @return token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 验证JWT token
     *
     * @param token token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从token中获取用户ID
     *
     * @param token token
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        if (claims != null) {
            return (String) claims.get("userId");
        }
        return null;
    }

    /**
     * 从token中获取用户名
     *
     * @param token token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        if (claims != null) {
            return (String) claims.get("username");
        }
        return null;
    }

    /**
     * 获取token中的所有声明
     *
     * @param token token
     * @return 声明
     */
    public Claims getAllClaimsFromToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 获取token过期时间（秒）
     *
     * @return 过期时间
     */
    public Long getExpiresIn() {
        return jwtExpiration / 1000;
    }
}
