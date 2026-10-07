package com.achat.report;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class ReportStore {
    private final ReportRepository repository;

    public void save(Report report) { repository.save(report); }
    public Optional<Report> findById(String id) { return repository.findById(id); }
    public int getUserReportCount(String id) { return count(ReportType.USER, id); }
    public int getRoomReportCount(String id) { return count(ReportType.ROOM, id); }
    public int getPostReportCount(String id) { return count(ReportType.POST, id); }
    public int getCommentReportCount(String id) { return count(ReportType.COMMENT, id); }
    private int count(ReportType type, String id) { return Math.toIntExact(repository.countByTargetTypeAndTargetId(type, id)); }
    public boolean hasRecentReport(String reporterId, String targetId) {
        return repository.existsByReporterIdAndTargetIdAndCreatedAtAfter(reporterId, targetId, Instant.now().minus(Duration.ofHours(24)));
    }
    public List<Report> findByReporterId(String id) { return repository.findByReporterIdOrderByCreatedAtDesc(id); }
    public List<Report> findByStatus(ReportStatus status) { return repository.findByStatusOrderByCreatedAtDesc(status); }
    public List<Report> findAll() { return repository.findAllByOrderByCreatedAtDesc(); }
    public List<Report> findByTargetId(String id) { return repository.findByTargetIdOrderByCreatedAtDesc(id); }
}
