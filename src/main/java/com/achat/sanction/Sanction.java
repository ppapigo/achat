package com.achat.sanction;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sanctions", indexes = @Index(columnList = "user_id,issued_at"))
public class Sanction extends JpaEntity {
    protected Sanction() {}

    @Id
    @Column(length = 36)
    private String id;
    @Column(length = 36)
    private String userId;
    @Enumerated(EnumType.STRING)
    private SanctionType type;
    @Column(columnDefinition = "TEXT")
    private String reason;
    @Column(length = 36)
    private String reportId;  // 관련 신고 ID (nullable)
    private String issuedBy;  // 관리자 ID
    private Instant issuedAt;
    private Instant expiresAt; // null이면 영구
    private boolean isActive;

    public Sanction(String userId, SanctionType type, String reason,
                    String reportId, String issuedBy, Integer durationMinutes) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.type = type;
        this.reason = reason;
        this.reportId = reportId;
        this.issuedBy = issuedBy;
        this.issuedAt = Instant.now();

        // 기간 계산
        if (durationMinutes == null || durationMinutes < 0) {
            this.expiresAt = null; // 영구
        } else if (durationMinutes == 0) {
            this.expiresAt = Instant.now(); // 즉시 만료 (경고)
        } else {
            this.expiresAt = Instant.now().plusSeconds(durationMinutes * 60L);
        }

        this.isActive = true;
    }

    // 제재 해제
    public void revoke() {
        this.isActive = false;
    }

    // 아직 유효한지 체크
    public boolean isEffective() {
        if (!isActive) return false;
        if (expiresAt == null) return true; // 영구
        return Instant.now().isBefore(expiresAt);
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public SanctionType getType() { return type; }
    public String getReason() { return reason; }
    public String getReportId() { return reportId; }
    public String getIssuedBy() { return issuedBy; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isActive() { return isActive; }
}