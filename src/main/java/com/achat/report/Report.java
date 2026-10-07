package com.achat.report;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports", indexes = @Index(columnList = "target_type,target_id"))
public class Report extends JpaEntity {
    protected Report() {}

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
    private ReportStatus status;
    @Column(length = 36)
    private String roomId;  // 신고가 발생한 방 (nullable)
    private Instant createdAt;
    private Instant resolvedAt;
    @Column(columnDefinition = "TEXT")
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

    // Getters
    public String getId() { return id; }
    public String getReporterId() { return reporterId; }
    public ReportType getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public ReportReason getReason() { return reason; }
    public String getDescription() { return description; }
    public ReportStatus getStatus() { return status; }
    public String getRoomId() { return roomId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getAdminNote() { return adminNote; }

    // Setters
    public void setStatus(ReportStatus status) { this.status = status; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }

    public void resolve(ReportStatus status, String adminNote) {
        this.status = status;
        this.adminNote = adminNote;
        this.resolvedAt = Instant.now();
    }
}