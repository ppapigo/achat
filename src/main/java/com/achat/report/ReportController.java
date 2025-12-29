package com.achat.report;

import com.achat.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 유저 신고
     */
    @PostMapping("/user")
    public ResponseEntity<ApiResponse<ReportService.ReportResult>> reportUser(
            HttpServletRequest request,
            @RequestBody ReportUserRequest body) {
        String userId = (String) request.getAttribute("userId");

        ReportService.ReportResult result = reportService.reportUser(
                userId,
                body.targetUserId(),
                body.reason(),
                body.description(),
                body.roomId()
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 방 신고
     */
    @PostMapping("/room")
    public ResponseEntity<ApiResponse<ReportService.ReportResult>> reportRoom(
            HttpServletRequest request,
            @RequestBody ReportRoomRequest body) {
        String userId = (String) request.getAttribute("userId");

        ReportService.ReportResult result = reportService.reportRoom(
                userId,
                body.targetRoomId(),
                body.reason(),
                body.description()
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 게시글 신고
     */
    @PostMapping("/post")
    public ResponseEntity<ApiResponse<ReportService.ReportResult>> reportPost(
            HttpServletRequest request,
            @RequestBody ReportPostRequest body) {
        String userId = (String) request.getAttribute("userId");

        ReportService.ReportResult result = reportService.reportPost(
                userId,
                body.postId(),
                body.reason(),
                body.description()
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 유저 신고 사유 목록
     */
    @GetMapping("/reasons/user")
    public ResponseEntity<ApiResponse<List<ReportService.ReasonResponse>>> getUserReasons() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getUserReportReasons()));
    }

    /**
     * 방 신고 사유 목록
     */
    @GetMapping("/reasons/room")
    public ResponseEntity<ApiResponse<List<ReportService.ReasonResponse>>> getRoomReasons() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getRoomReportReasons()));
    }

    /**
     * 게시글 신고 사유 목록
     */
    @GetMapping("/reasons/post")
    public ResponseEntity<ApiResponse<List<ReportService.ReasonResponse>>> getPostReasons() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getPostReportReasons()));
    }

    /**
     * 내가 한 신고 목록
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Report>>> getMyReports(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        List<Report> reports = reportService.getMyReports(userId);
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    /**
     * 댓글 신고
     */
    @PostMapping("/comment")
    public ResponseEntity<ApiResponse<ReportService.ReportResult>> reportComment(
            HttpServletRequest request,
            @RequestBody ReportCommentRequest body) {
        String userId = (String) request.getAttribute("userId");

        ReportService.ReportResult result = reportService.reportComment(
                userId,
                body.commentId(),
                body.reason(),
                body.description()
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 댓글 신고 사유 목록
     */
    @GetMapping("/reasons/comment")
    public ResponseEntity<ApiResponse<List<ReportService.ReasonResponse>>> getCommentReasons() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getCommentReportReasons()));
    }

    // DTO 추가
    public record ReportCommentRequest(
            String commentId,
            ReportReason reason,
            String description
    ) {}
    // DTOs
    public record ReportUserRequest(
            String targetUserId,
            ReportReason reason,
            String description,
            String roomId
    ) {}

    public record ReportRoomRequest(
            String targetRoomId,
            ReportReason reason,
            String description
    ) {}

    public record ReportPostRequest(
            String postId,
            ReportReason reason,
            String description
    ) {}
}