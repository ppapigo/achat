package com.achat.report;

import java.time.Instant;
import java.util.UUID;

public class Report {
    private final String id;
    private final String reporterId;
    private final ReportType targetType;
    private final String targetId;
    private final ReportReason reason;
    private String description;
    private ReportStatus status;
    private final String roomId;  // 신고가 발생한 방 (nullable)
    private final Instant createdAt;
    private Instant resolvedAt;
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