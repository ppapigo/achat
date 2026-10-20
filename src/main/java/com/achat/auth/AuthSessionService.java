package com.achat.auth;

import com.achat.auth.dto.TokenResponse;
import com.achat.security.UnauthorizedException;
import com.achat.sanction.SanctionService;
import com.achat.user.*;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthSessionService {
    private final AuthSessionRepository sessions;
    private final JwtProvider jwt;
    private final UserStore users;
    private final SanctionService sanctions;

    public record Identity(String userId, String sessionId, Instant expiresAt) implements Principal {
        @Override public String getName() { return userId; }
    }

    @Transactional
    public TokenResponse issue(User user) {
        requireAllowed(user.getId());
        AuthSession session = new AuthSession(user.getId(), hash(user.getPasswordHash()), jwt.refreshExpiry());
        TokenResponse tokens = tokens(user, session);
        sessions.save(session);
        return tokens;
    }

    // Reuse revokes the family. The revocation must commit even when the request is rejected.
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = UnauthorizedException.class)
    public TokenResponse refresh(String token) {
        Claims claims = jwt.parse(token, "refresh");
        AuthSession session = sessions.lockById(claims.get("sid", String.class)).orElseThrow(UnauthorizedException::new);
        User user = validUser(session, claims.getSubject());
        requireAllowed(user.getId());
        if (!MessageDigest.isEqual(hash(token).getBytes(StandardCharsets.US_ASCII),
                session.getRefreshHash().getBytes(StandardCharsets.US_ASCII))) {
            session.revoke();
            throw new UnauthorizedException();
        }
        return tokens(user, session);
    }

    @Transactional(readOnly = true)
    public Identity authenticate(String token) {
        Claims claims = jwt.parse(token, "access");
        Identity identity = new Identity(claims.getSubject(), claims.get("sid", String.class),
                claims.getExpiration().toInstant());
        validate(identity);
        return identity;
    }

    @Transactional(readOnly = true)
    public User validate(Identity identity) {
        if (identity == null || !identity.expiresAt().isAfter(Instant.now())) throw new UnauthorizedException();
        AuthSession session = sessions.findById(identity.sessionId()).orElseThrow(UnauthorizedException::new);
        User user = validUser(session, identity.userId());
        requireAllowed(user.getId());
        return user;
    }

    @Transactional
    public void logout(String token) {
        Claims claims = jwt.parse(token, "refresh");
        sessions.lockById(claims.get("sid", String.class)).ifPresent(session -> {
            if (!session.getUserId().equals(claims.getSubject())) throw new UnauthorizedException();
            session.revoke();
        });
    }
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 3_600_000, initialDelay = 3_600_000)
    @Transactional
    public void cleanExpiredSessions() { sessions.deleteExpired(Instant.now()); }
    private User validUser(AuthSession session, String userId) {
        if (session.isRevoked() || !session.getUserId().equals(userId)
                || !session.getExpiresAt().isAfter(Instant.now())) throw new UnauthorizedException();
        User user = users.findById(userId).orElseThrow(UnauthorizedException::new);
        if (!session.getPasswordFingerprint().equals(hash(user.getPasswordHash()))) throw new UnauthorizedException();
        return user;
    }
    private void requireAllowed(String userId) {
        if (sanctions.isBanned(userId)) throw new AccessDeniedException("정지된 계정입니다");
    }
    private TokenResponse tokens(User user, AuthSession session) {
        String refresh = jwt.refresh(user.getId(), session.getId(), session.getExpiresAt());
        session.rotate(hash(refresh));
        return new TokenResponse(jwt.access(user.getId(), session.getId()), refresh, user.getId(), user.getEmail());
    }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
