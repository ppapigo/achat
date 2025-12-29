package com.achat.report;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ReportStore {

    private final Map<String, Report> reports = new ConcurrentHashMap<>();

    // 타입별 인덱스
    private final Map<String, List<String>> userReports = new ConcurrentHashMap<>();
    private final Map<String, List<String>> roomReports = new ConcurrentHashMap<>();
    private final Map<String, List<String>> postReports = new ConcurrentHashMap<>();
    private final Map<String, List<String>> commentReports = new ConcurrentHashMap<>();  // 추가!
    private final Map<String, List<String>> reporterReports = new ConcurrentHashMap<>();

    public void save(Report report) {
        reports.put(report.getId(), report);

        // 타입별 인덱스 업데이트
        switch (report.getTargetType()) {
            case USER -> userReports.computeIfAbsent(report.getTargetId(), k -> new ArrayList<>())
                    .add(report.getId());
            case ROOM -> roomReports.computeIfAbsent(report.getTargetId(), k -> new ArrayList<>())
                    .add(report.getId());
            case POST -> postReports.computeIfAbsent(report.getTargetId(), k -> new ArrayList<>())
                    .add(report.getId());
            case COMMENT -> commentReports.computeIfAbsent(report.getTargetId(), k -> new ArrayList<>())
                    .add(report.getId());
        }

        reporterReports.computeIfAbsent(report.getReporterId(), k -> new ArrayList<>())
                .add(report.getId());
    }

    public Optional<Report> findById(String id) {
        return Optional.ofNullable(reports.get(id));
    }

    public int getUserReportCount(String userId) {
        return userReports.getOrDefault(userId, List.of()).size();
    }

    public int getRoomReportCount(String roomId) {
        return roomReports.getOrDefault(roomId, List.of()).size();
    }

    public int getPostReportCount(String postId) {
        return postReports.getOrDefault(postId, List.of()).size();
    }

    public int getCommentReportCount(String commentId) {
        return commentReports.getOrDefault(commentId, List.of()).size();
    }

    public boolean hasRecentReport(String reporterId, String targetId) {
        List<String> myReports = reporterReports.getOrDefault(reporterId, List.of());
        Instant oneDayAgo = Instant.now().minus(Duration.ofHours(24));

        return myReports.stream()
                .map(reports::get)
                .filter(Objects::nonNull)
                .filter(r -> r.getTargetId().equals(targetId))
                .anyMatch(r -> r.getCreatedAt().isAfter(oneDayAgo));
    }

    public List<Report> findByReporterId(String reporterId) {
        return reporterReports.getOrDefault(reporterId, List.of()).stream()
                .map(reports::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Report> findByStatus(ReportStatus status) {
        return reports.values().stream()
                .filter(r -> r.getStatus() == status)
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Report> findAll() {
        return reports.values().stream()
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Report> findByTargetId(String targetId) {
        List<String> all = new ArrayList<>();
        all.addAll(userReports.getOrDefault(targetId, List.of()));
        all.addAll(roomReports.getOrDefault(targetId, List.of()));
        all.addAll(postReports.getOrDefault(targetId, List.of()));
        all.addAll(commentReports.getOrDefault(targetId, List.of()));

        return all.stream()
                .map(reports::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Report::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
}