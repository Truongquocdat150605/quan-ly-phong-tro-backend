package com.example.quanliPT.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration;

    // ✅ Tạo signing key từ raw string (không cần base64)
    private Key getSignInKey() {
        String secret = jwtSecret;
        // Đảm bảo secret đủ dài cho HMAC-SHA256 (32 bytes minimum)
        if (secret.length() < 32) {
            secret = secret + "0".repeat(32 - secret.length());
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ✅ Tạo token với roles nhúng vào claim (Stateless)
    public String generateToken(UserDetails userDetails) {
        // Lấy tất cả roles từ UserDetails và nhúng vào token
        String roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("roles", roles)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ Lấy username từ token
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // ✅ Lấy roles từ token (không cần truy vấn DB)
    public String extractRole(String token) {
        try {
            return parseClaims(token).get("roles", String.class);
        } catch (Exception e) {
            System.err.println("❌ JWT extractRole error: " + e.getMessage());
            return null;
        }
    }

    // ✅ Validate token mà KHÔNG cần UserDetails (Stateless)
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println("⚠️ JWT expired: " + e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("❌ JWT invalid: " + e.getMessage());
        }
        return false;
    }

    // ✅ Giữ backward-compatible cho code cũ (dùng UserDetails)
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && validateToken(token);
    }

    // 🔑 Parse claims nội bộ
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
