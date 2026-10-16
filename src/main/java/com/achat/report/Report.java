package com.achat.report;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports", indexes = @Index(columnList = "target_type,target_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Report extends JpaEntity {

    @Id
    @Column(length = 36)
    private String id;
    @Column(length = 36)
    private String reporterId;
    @Enumerated(EnumType.STRING)
    private ReportType targetType;
    @Column(length = 36)
    private String targetId;
    @Enumerated(EnumType.STRING)
    private ReportReason reason;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING)
    @Setter
    private ReportStatus status;
    @Column(length = 36)
    private String roomId;  // 신고가 발생한 방 (nullable)
    private Instant createdAt;
    @Setter
    private Instant resolvedAt;
    @Column(columnDefinition = "TEXT")
    @Setter
    private String adminNote;  // 관리자 메모

    public Report(String reporterId, ReportType targetType, String targetId,
                  ReportReason reason, String description, String roomId) {
        this.id = UUID.randomUUID().toString();
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.description = description;
        this.roomId = roomId;
        this.status = ReportStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void resolve(ReportStatus status, String adminNote) {
        this.status = status;
        this.adminNote = adminNote;
        this.resolvedAt = Instant.now();
    }
}
