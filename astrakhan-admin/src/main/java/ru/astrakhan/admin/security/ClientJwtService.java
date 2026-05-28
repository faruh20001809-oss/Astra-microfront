package ru.astrakhan.admin.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ClientJwtService {

    private final SecretKey secretKey;
    private final long expirationSeconds;

    public ClientJwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-hours:168}") long expirationHours) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 characters");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = Math.max(1, expirationHours) * 3600L;
    }

    public String createToken(Long userId, String email, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("username", username)
                .claim("type", "client")
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(secretKey)
                .compact();
    }

    public ClientPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"client".equals(claims.get("type"))) {
                throw new JwtException("Invalid token type");
            }
            Long userId = Long.parseLong(claims.getSubject());
            String email = claims.get("email", String.class);
            String username = claims.get("username", String.class);
            if (email == null || email.isBlank()) {
                throw new JwtException("Missing email claim");
            }
            return new ClientPrincipal(userId, email.trim().toLowerCase(), username != null ? username : "");
        } catch (JwtException | NumberFormatException e) {
            throw new JwtException("Invalid client token", e);
        }
    }

    public Map<String, Object> tokenMeta(String accessToken) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("accessToken", accessToken);
        meta.put("tokenType", "Bearer");
        meta.put("expiresIn", expirationSeconds);
        return meta;
    }
}
