package com.achat.auth;

import com.achat.security.UnauthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {
    private final SecretKey key;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public JwtProvider(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs,
                       @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs,
                       Environment environment) {
        boolean production = environment.acceptsProfiles(Profiles.of("prod"));
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32
                || (production && (secret.length() < 64 || secret.chars().distinct().count() < 16
                || secret.toLowerCase(java.util.Locale.ROOT).matches(".*(example|changeme|test.secret|dev.secret).*")))) {
            throw new IllegalStateException("JWT_SECRET must be a strong, independently generated secret (production: 64+ characters)");
        }
        if (accessExpirationMs <= 0 || accessExpirationMs > 900_000
                || refreshExpirationMs < accessExpirationMs || refreshExpirationMs > 604_800_000) {
            throw new IllegalStateException("Invalid JWT lifetime configuration");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }
    public Instant refreshExpiry() { return Instant.now().plusMillis(refreshExpirationMs); }
    String access(String userId, String sessionId) {
        return token(userId, sessionId, "access", Instant.now().plusMillis(accessExpirationMs));
    }
    String refresh(String userId, String sessionId, Instant expiresAt) {
        return token(userId, sessionId, "refresh", expiresAt);
    }
    private String token(String userId, String sessionId, String type, Instant expiresAt) {
        return Jwts.builder().issuer("achat").subject(userId).id(UUID.randomUUID().toString())
                .claim("sid", sessionId).claim("type", type)
                .issuedAt(new Date()).expiration(Date.from(expiresAt)).signWith(key).compact();
    }
    public Claims parse(String token, String requiredType) {
        try {
            if (token == null || token.length() > 4096) throw new UnauthorizedException();
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer("achat").require("type", requiredType)
                    .build().parseSignedClaims(token).getPayload();
            UUID.fromString(claims.getSubject());
            UUID.fromString(claims.get("sid", String.class));
            UUID.fromString(claims.getId());
            if (claims.getExpiration() == null || claims.getIssuedAt() == null
                    || claims.getIssuedAt().toInstant().isAfter(Instant.now().plusSeconds(30))) throw new UnauthorizedException();
            return claims;
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new UnauthorizedException();
        }
    }
}
