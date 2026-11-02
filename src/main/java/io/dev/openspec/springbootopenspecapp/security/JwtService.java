package io.dev.openspec.springbootopenspecapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final int MIN_KEY_BYTES = 32;

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException("app.security.jwt.secret must be configured");
        }
        byte[] keyBytes = Decoders.BASE64.decode(properties.secret());
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("app.security.jwt.secret must have at least 256 bits (32 bytes, Base64 encoded)");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = properties.expiration();
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject() != null && !claims.getSubject().isBlank();
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Duration getExpiration() {
        return expiration;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
