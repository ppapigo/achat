package com.achat.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface ReportRepository extends JpaRepository<Report, String> {
    long countByTargetTypeAndTargetId(ReportType targetType, String targetId);
    boolean existsByReporterIdAndTargetIdAndCreatedAtAfter(String reporterId, String targetId, Instant cutoff);
    List<Report> findByReporterIdOrderByCreatedAtDesc(String reporterId);
    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);
    List<Report> findAllByOrderByCreatedAtDesc();
    List<Report> findByTargetIdOrderByCreatedAtDesc(String targetId);
}
