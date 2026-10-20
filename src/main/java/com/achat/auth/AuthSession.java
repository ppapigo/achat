package com.achat.auth;

import com.achat.persistence.JpaEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_sessions", indexes = {
        @Index(name = "idx_auth_session_user", columnList = "userId"),
        @Index(name = "idx_auth_session_expiry", columnList = "expiresAt")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSession extends JpaEntity {
    @Id @Column(length = 36) private String id;
    @Column(nullable = false, length = 36) private String userId;
    @Column(nullable = false, length = 64) private String passwordFingerprint;
    @Column(nullable = false, length = 64) private String refreshHash;
    @Column(nullable = false) private Instant expiresAt;
    @Column(nullable = false) private boolean revoked;

    AuthSession(String userId, String passwordFingerprint, Instant expiresAt) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.passwordFingerprint = passwordFingerprint;
        this.expiresAt = expiresAt;
    }
    void rotate(String refreshHash) { this.refreshHash = refreshHash; }
    void revoke() { revoked = true; }
}
