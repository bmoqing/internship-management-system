package fun.bmoqing.internship.security;

import fun.bmoqing.internship.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessExpireMs;
    private final long refreshExpireMs;

    public JwtUtil(@Value("${jwt.secret:internship-jwt-secret-key-change-me-123456}") String secret,
                   @Value("${jwt.expire-ms:86400000}") long accessExpireMs,
                   @Value("${jwt.refresh-expire-ms:604800000}") long refreshExpireMs) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.secretKey = Keys.hmacShaKeyFor(normalizeKeyBytes(keyBytes));
        this.accessExpireMs = accessExpireMs;
        this.refreshExpireMs = refreshExpireMs;
    }

    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    public String generateAccessToken(User user) {
        return buildToken(user, accessExpireMs, "ACCESS"); //提供了生成 Access Token
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpireMs, "REFRESH");
    }//Refresh Token

    private String buildToken(User user, long expireMs, String tokenType) {
        Instant now = Instant.now();
        Instant expireAt = now.plusMillis(expireMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole())
                .claim("username", user.getUsername())
                .claim("tokenType", tokenType)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(secretKey)
                .compact();
    }
//解析token
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(String token) {
        String sub = parseClaims(token).getSubject();
        return sub == null ? null : Long.parseLong(sub);
    }

    public String getRole(String token) {
        Object role = parseClaims(token).get("role");
        return role == null ? null : role.toString();
    }

    public String getTokenType(String token) {
        Object tokenType = parseClaims(token).get("tokenType");
        return tokenType == null ? null : tokenType.toString();
    }

    public long getExpireAtMillis(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration == null ? 0L : expiration.getTime();
    }

    public long getAccessExpireMs() {
        return accessExpireMs;
    }

    public long getRefreshExpireMs() {
        return refreshExpireMs;
    }

    private byte[] normalizeKeyBytes(byte[] keyBytes) {
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        byte[] normalized = new byte[32];
        System.arraycopy(keyBytes, 0, normalized, 0, keyBytes.length);
        for (int i = keyBytes.length; i < normalized.length; i++) {
            normalized[i] = (byte) '0';
        }
        return normalized;
    }
}
