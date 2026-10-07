package com.achat.report;

import org.springframework.transaction.annotation.Transactional;

import com.achat.board.BoardService;
import com.achat.sanction.SanctionService;
import com.achat.sanction.SanctionType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class ReportService {

    private final ReportStore reportStore;
    private final SanctionService sanctionService;
    private final BoardService boardService;  // ✅ 추가

    // 자동 제재 임계값
    private static final int USER_WARNING_THRESHOLD = 3;
    private static final int USER_MUTE_THRESHOLD = 5;
    private static final int USER_TEMP_BAN_THRESHOLD = 10;
    private static final int USER_PERM_BAN_THRESHOLD = 20;

    private static final int POST_HIDE_THRESHOLD = 5;
    private static final int ROOM_WARNING_THRESHOLD = 10;

    private static final int COMMENT_WARNING_THRESHOLD = 3;
    private static final int COMMENT_ACTION_THRESHOLD = 5;

    // ✅ 생성자 수정
    public ReportService(ReportStore reportStore, SanctionService sanctionService, BoardService boardService) {
        this.reportStore = reportStore;
        this.sanctionService = sanctionService;
        this.boardService = boardService;
    }

    /**
     * 유저 신고
     */
    public ReportResult reportUser(String reporterId, String targetUserId,
                                   ReportReason reason, String description, String roomId) {
        if (reporterId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신을 신고할 수 없습니다");
        }

        if (reportStore.hasRecentReport(reporterId, targetUserId)) {
            throw new IllegalStateException("이미 최근에 신고한 유저입니다");
        }

        Report report = new Report(reporterId, ReportType.USER, targetUserId,
                reason, description, roomId);
        reportStore.save(report);

        int totalReports = reportStore.getUserReportCount(targetUserId);
        String warning = applyAutoSanction(targetUserId, totalReports);

        System.out.println("🚨 유저 신고: " + targetUserId + " (사유: " + reason.getDescription() + ")");
        System.out.println("   누적 신고: " + totalReports + "회");

        return new ReportResult(report.getId(), totalReports, warning);
    }

    /**
     * 자동 제재 적용
     */
    private String applyAutoSanction(String userId, int totalReports) {
        try {
            if (sanctionService.isBanned(userId)) {
                return "ALREADY_BANNED";
            }

            if (totalReports >= USER_PERM_BAN_THRESHOLD) {
                if (!sanctionService.hasActiveSanctionType(userId, SanctionType.PERM_BAN)) {
                    sanctionService.applySanction(userId, SanctionType.PERM_BAN,
                            "자동 제재: 신고 " + totalReports + "회 누적", null, "SYSTEM", null);
                    System.out.println("⚖️ 자동 영구정지: " + userId);
                    return "AUTO_PERM_BAN";
                }
            } else if (totalReports >= USER_TEMP_BAN_THRESHOLD) {
                if (!sanctionService.hasActiveSanctionType(userId, SanctionType.TEMP_BAN)) {
                    sanctionService.applySanction(userId, SanctionType.TEMP_BAN,
                            "자동 제재: 신고 " + totalReports + "회 누적", null, "SYSTEM", 1440);
                    System.out.println("⚖️ 자동 임시정지(24h): " + userId);
                    return "AUTO_TEMP_BAN";
                }
            } else if (totalReports >= USER_MUTE_THRESHOLD) {
                if (!sanctionService.hasActiveSanctionType(userId, SanctionType.MUTE)) {
                    sanctionService.applySanction(userId, SanctionType.MUTE,
                            "자동 제재: 신고 " + totalReports + "회 누적", null, "SYSTEM", 60);
                    System.out.println("⚖️ 자동 채금(1h): " + userId);
                    return "AUTO_MUTE";
                }
            } else if (totalReports >= USER_WARNING_THRESHOLD) {
                if (!sanctionService.hasActiveSanctionType(userId, SanctionType.WARNING)) {
                    sanctionService.applySanction(userId, SanctionType.WARNING,
                            "자동 제재: 신고 " + totalReports + "회 누적", null, "SYSTEM", 0);
                    System.out.println("⚖️ 자동 경고: " + userId);
                    return "AUTO_WARNING";
                }
            }
        } catch (Exception e) {
            System.out.println("자동 제재 적용 실패: " + e.getMessage());
        }
        return null;
    }

    /**
     * 댓글 신고 ✅ 수정: 자동 숨김
     */
    public ReportResult reportComment(String reporterId, String commentId,
                                      ReportReason reason, String description) {
        if (reportStore.hasRecentReport(reporterId, commentId)) {
            throw new IllegalStateException("이미 최근에 신고한 댓글입니다");
        }

        Report report = new Report(reporterId, ReportType.COMMENT, commentId,
                reason, description, null);
        reportStore.save(report);

        int totalReports = reportStore.getCommentReportCount(commentId);
        String warning = null;

        // ✅ 5회 이상 신고 시 자동 숨김
        if (totalReports >= COMMENT_ACTION_THRESHOLD) {
            try {
                Long cmtId = Long.parseLong(commentId);
                boardService.hideComment(cmtId, "신고 " + totalReports + "회 누적으로 자동 숨김");
                warning = "COMMENT_HIDDEN";
                System.out.println("🙈 댓글 자동 숨김: " + commentId);
            } catch (NumberFormatException e) {
                warning = "COMMENT_REVIEW_REQUIRED";
            }
        } else if (totalReports >= COMMENT_WARNING_THRESHOLD) {
            warning = "COMMENT_WARNING";
        }

        System.out.println("🚨 댓글 신고: " + commentId + " (사유: " + reason.getDescription() + ")");
        System.out.println("   누적 신고: " + totalReports + "회");

        return new ReportResult(report.getId(), totalReports, warning);
    }

    /**
     * 방 신고
     */
    public ReportResult reportRoom(String reporterId, String targetRoomId,
                                   ReportReason reason, String description) {
        if (reportStore.hasRecentReport(reporterId, targetRoomId)) {
            throw new IllegalStateException("이미 최근에 신고한 방입니다");
        }

        Report report = new Report(reporterId, ReportType.ROOM, targetRoomId,
                reason, description, targetRoomId);
        reportStore.save(report);

        int totalReports = reportStore.getRoomReportCount(targetRoomId);
        String warning = null;

        if (totalReports >= ROOM_WARNING_THRESHOLD) {
            warning = "ROOM_REVIEW_REQUIRED";
            System.out.println("⚠️ 방 검토 필요: " + targetRoomId + " (신고 " + totalReports + "회)");
        }

        System.out.println("🚨 방 신고: " + targetRoomId + " (사유: " + reason.getDescription() + ")");
        System.out.println("   누적 신고: " + totalReports + "회");

        return new ReportResult(report.getId(), totalReports, warning);
    }

    /**
     * 게시글 신고 ✅ 수정: 자동 숨김
     */
    public ReportResult reportPost(String reporterId, String postId,
                                   ReportReason reason, String description) {
        if (reportStore.hasRecentReport(reporterId, postId)) {
            throw new IllegalStateException("이미 최근에 신고한 게시글입니다");
        }

        Report report = new Report(reporterId, ReportType.POST, postId,
                reason, description, null);
        reportStore.save(report);

        int totalReports = reportStore.getPostReportCount(postId);
        String warning = null;

        // ✅ 5회 이상 신고 시 자동 숨김
        if (totalReports >= POST_HIDE_THRESHOLD) {
            try {
                Long boardId = Long.parseLong(postId);
                boardService.hideBoard(boardId, "신고 " + totalReports + "회 누적으로 자동 숨김");
                warning = "POST_HIDDEN";
                System.out.println("🙈 게시글 자동 숨김: " + postId);
            } catch (NumberFormatException e) {
                warning = "POST_REVIEW_REQUIRED";
            }
        } else if (totalReports >= 3) {
            warning = "POST_WARNING";
        }

        System.out.println("🚨 게시글 신고: " + postId + " (사유: " + reason.getDescription() + ")");
        System.out.println("   누적 신고: " + totalReports + "회");

        return new ReportResult(report.getId(), totalReports, warning);
    }

    /**
     * 내가 한 신고 목록
     */
    public List<Report> getMyReports(String userId) {
        return reportStore.findByReporterId(userId);
    }

    /**
     * 대기 중인 신고 목록 (관리자용)
     */
    public List<Report> getPendingReports() {
        return reportStore.findByStatus(ReportStatus.PENDING);
    }

    /**
     * 상태별 신고 목록 (관리자용)
     */
    public List<Report> getReportsByStatus(ReportStatus status) {
        return reportStore.findByStatus(status);
    }

    /**
     * 전체 신고 목록 (관리자용)
     */
    public List<Report> getAllReports() {
        return reportStore.findAll();
    }

    /**
     * 단일 신고 조회
     */
    public Report getReport(String reportId) {
        return reportStore.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));
    }

    /**
     * 신고 처리 (관리자용)
     */
    public Report resolveReport(String reportId, ReportStatus status, String adminNote) {
        Report report = reportStore.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("신고를 찾을 수 없습니다"));

        report.resolve(status, adminNote);
        return report;
    }

    /**
     * 유저 신고 사유 목록
     */
    public List<ReasonResponse> getUserReportReasons() {
        return List.of(
                new ReasonResponse(ReportReason.HARASSMENT, ReportReason.HARASSMENT.getDescription()),
                new ReasonResponse(ReportReason.SPAM, ReportReason.SPAM.getDescription()),
                new ReasonResponse(ReportReason.INAPPROPRIATE_CONTENT, ReportReason.INAPPROPRIATE_CONTENT.getDescription()),
                new ReasonResponse(ReportReason.IMPERSONATION, ReportReason.IMPERSONATION.getDescription()),
                new ReasonResponse(ReportReason.CHEATING, ReportReason.CHEATING.getDescription()),
                new ReasonResponse(ReportReason.OTHER, ReportReason.OTHER.getDescription())
        );
    }

    /**
     * 방 신고 사유 목록
     */
    public List<ReasonResponse> getRoomReportReasons() {
        return List.of(
                new ReasonResponse(ReportReason.ILLEGAL_CONTENT, ReportReason.ILLEGAL_CONTENT.getDescription()),
                new ReasonResponse(ReportReason.INAPPROPRIATE_NAME, ReportReason.INAPPROPRIATE_NAME.getDescription()),
                new ReasonResponse(ReportReason.SCAM, ReportReason.SCAM.getDescription()),
                new ReasonResponse(ReportReason.SPAM, ReportReason.SPAM.getDescription()),
                new ReasonResponse(ReportReason.OTHER, ReportReason.OTHER.getDescription())
        );
    }

    /**
     * 게시글 신고 사유 목록
     */
    public List<ReasonResponse> getPostReportReasons() {
        return List.of(
                new ReasonResponse(ReportReason.SPAM, ReportReason.SPAM.getDescription()),
                new ReasonResponse(ReportReason.INAPPROPRIATE_CONTENT, ReportReason.INAPPROPRIATE_CONTENT.getDescription()),
                new ReasonResponse(ReportReason.FALSE_INFO, ReportReason.FALSE_INFO.getDescription()),
                new ReasonResponse(ReportReason.ADVERTISEMENT, ReportReason.ADVERTISEMENT.getDescription()),
                new ReasonResponse(ReportReason.HATE_SPEECH, ReportReason.HATE_SPEECH.getDescription()),
                new ReasonResponse(ReportReason.PRIVACY_VIOLATION, ReportReason.PRIVACY_VIOLATION.getDescription()),
                new ReasonResponse(ReportReason.OTHER, ReportReason.OTHER.getDescription())
        );
    }

    /**
     * 댓글 신고 사유 목록
     */
    public List<ReasonResponse> getCommentReportReasons() {
        return List.of(
                new ReasonResponse(ReportReason.HARASSMENT, ReportReason.HARASSMENT.getDescription()),
                new ReasonResponse(ReportReason.SPAM, ReportReason.SPAM.getDescription()),
                new ReasonResponse(ReportReason.INAPPROPRIATE_CONTENT, ReportReason.INAPPROPRIATE_CONTENT.getDescription()),
                new ReasonResponse(ReportReason.HATE_SPEECH, ReportReason.HATE_SPEECH.getDescription()),
                new ReasonResponse(ReportReason.OTHER, ReportReason.OTHER.getDescription())
        );
    }

    public record ReportResult(String reportId, int totalReports, String warning) {}
    public record ReasonResponse(ReportReason reason, String description) {}
}