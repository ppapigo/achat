package com.achat.admin;

import com.achat.common.ApiResponse;
import com.achat.report.Report;
import com.achat.report.ReportService;
import com.achat.report.ReportStatus;
import com.achat.sanction.Sanction;
import com.achat.sanction.SanctionService;
import com.achat.sanction.SanctionType;
import com.achat.user.User;
import com.achat.user.UserStore;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReportService reportService;
    private final SanctionService sanctionService;
    private final UserStore userStore;

    public AdminController(ReportService reportService,
                           SanctionService sanctionService,
                           UserStore userStore) {
        this.reportService = reportService;
        this.sanctionService = sanctionService;
        this.userStore = userStore;
    }

    /**
     * 관리자 권한 체크
     */
    private void checkAdmin(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        User user = userStore.findById(userId)
                .orElseThrow(() -> new IllegalStateException("유저를 찾을 수 없습니다"));

        if (!user.isAdmin()) {
            throw new IllegalStateException("관리자 권한이 없습니다");
        }
    }

    // ==================== 신고 관리 ====================

    /**
     * 대기 중인 신고 목록
     */
    @GetMapping("/reports/pending")
    public ResponseEntity<ApiResponse<List<Report>>> getPendingReports(HttpServletRequest request) {
        checkAdmin(request);
        List<Report> reports = reportService.getPendingReports();
        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    /**
     * 전체 신고 목록
     */
    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<Report>>> getAllReports(
            HttpServletRequest request,
            @RequestParam(required = false) String status) {
        checkAdmin(request);

        List<Report> reports;
        if (status != null) {
            ReportStatus reportStatus = ReportStatus.valueOf(status.toUpperCase());
            reports = reportService.getReportsByStatus(reportStatus);
        } else {
            reports = reportService.getAllReports();
        }

        return ResponseEntity.ok(ApiResponse.ok(reports));
    }

    /**
     * 신고 상세 조회
     */
    @GetMapping("/reports/{reportId}")
    public ResponseEntity<ApiResponse<ReportDetail>> getReportDetail(
            HttpServletRequest request,
            @PathVariable String reportId) {
        checkAdmin(request);

        Report report = reportService.getReport(reportId);

        // 신고 대상 정보
        String targetName = "";
        if (report.getTargetType().name().equals("USER")) {
            targetName = userStore.findById(report.getTargetId())
                    .map(User::getUsername)
                    .orElse("알 수 없음");
        }

        // 신고자 정보
        String reporterName = userStore.findById(report.getReporterId())
                .map(User::getUsername)
                .orElse("알 수 없음");

        // 대상 유저의 제재 이력
        List<Sanction> targetSanctions = report.getTargetType().name().equals("USER")
                ? sanctionService.getUserSanctions(report.getTargetId())
                : List.of();

        ReportDetail detail = new ReportDetail(report, targetName, reporterName, targetSanctions);

        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    /**
     * 신고 처리 (제재 없이 처리만)
     */
    @PostMapping("/reports/{reportId}/resolve")
    public ResponseEntity<ApiResponse<Report>> resolveReport(
            HttpServletRequest request,
            @PathVariable String reportId,
            @RequestBody ResolveRequest body) {
        checkAdmin(request);

        Report report = reportService.resolveReport(reportId, body.status(), body.adminNote());
        return ResponseEntity.ok(ApiResponse.ok(report));
    }

    /**
     * 신고 처리 + 제재 적용
     */
    @PostMapping("/reports/{reportId}/resolve-with-sanction")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveWithSanction(
            HttpServletRequest request,
            @PathVariable String reportId,
            @RequestBody ResolveWithSanctionRequest body) {
        checkAdmin(request);
        String adminId = (String) request.getAttribute("userId");

        // 신고 처리
        Report report = reportService.resolveReport(reportId, ReportStatus.RESOLVED, body.adminNote());

        // 제재 적용
        Sanction sanction = sanctionService.applySanction(
                report.getTargetId(),
                body.sanctionType(),
                body.sanctionReason(),
                reportId,
                adminId,
                body.durationMinutes()
        );

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "report", report,
                "sanction", sanction
        )));
    }

    // ==================== 제재 관리 ====================

    /**
     * 활성 제재 목록
     */
    @GetMapping("/sanctions/active")
    public ResponseEntity<ApiResponse<List<Sanction>>> getActiveSanctions(HttpServletRequest request) {
        checkAdmin(request);
        List<Sanction> sanctions = sanctionService.getActiveSanctionsAll();
        return ResponseEntity.ok(ApiResponse.ok(sanctions));
    }

    /**
     * 전체 제재 목록
     */
    @GetMapping("/sanctions")
    public ResponseEntity<ApiResponse<List<Sanction>>> getAllSanctions(HttpServletRequest request) {
        checkAdmin(request);
        List<Sanction> sanctions = sanctionService.getAllSanctions();
        return ResponseEntity.ok(ApiResponse.ok(sanctions));
    }

    /**
     * 제재 해제
     */
    @PostMapping("/sanctions/{sanctionId}/revoke")
    public ResponseEntity<ApiResponse<Sanction>> revokeSanction(
            HttpServletRequest request,
            @PathVariable String sanctionId) {
        checkAdmin(request);
        String adminId = (String) request.getAttribute("userId");

        Sanction sanction = sanctionService.revokeSanction(sanctionId, adminId);
        return ResponseEntity.ok(ApiResponse.ok(sanction));
    }

    /**
     * 직접 제재 (신고 없이)
     */
    @PostMapping("/sanctions")
    public ResponseEntity<ApiResponse<Sanction>> applySanction(
            HttpServletRequest request,
            @RequestBody ApplySanctionRequest body) {
        checkAdmin(request);
        String adminId = (String) request.getAttribute("userId");

        Sanction sanction = sanctionService.applySanction(
                body.userId(),
                body.type(),
                body.reason(),
                null,
                adminId,
                body.durationMinutes()
        );

        return ResponseEntity.ok(ApiResponse.ok(sanction));
    }

    /**
     * 제재 타입 목록
     */
    @GetMapping("/sanctions/types")
    public ResponseEntity<ApiResponse<List<SanctionService.SanctionTypeInfo>>> getSanctionTypes(
            HttpServletRequest request) {
        checkAdmin(request);
        return ResponseEntity.ok(ApiResponse.ok(sanctionService.getSanctionTypes()));
    }

    /**
     * 특정 유저의 제재 이력
     */
    @GetMapping("/users/{userId}/sanctions")
    public ResponseEntity<ApiResponse<List<Sanction>>> getUserSanctions(
            HttpServletRequest request,
            @PathVariable String userId) {
        checkAdmin(request);
        List<Sanction> sanctions = sanctionService.getUserSanctions(userId);
        return ResponseEntity.ok(ApiResponse.ok(sanctions));
    }
    // ==================== 유저 관리 ====================

    /**
     * 유저 검색
     */
    @GetMapping("/users/search")
    public ResponseEntity<ApiResponse<List<UserInfo>>> searchUsers(
            HttpServletRequest request,
            @RequestParam String q) {
        checkAdmin(request);

        List<UserInfo> users = userStore.searchByUsername(q).stream()
                .map(u -> new UserInfo(
                        u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.isAdmin(),
                        sanctionService.isBanned(u.getId()),
                        !sanctionService.canChat(u.getId())
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    /**
     * 유저 상세 정보
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDetail>> getUserDetail(
            HttpServletRequest request,
            @PathVariable String userId) {
        checkAdmin(request);

        User user = userStore.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));

        List<Sanction> sanctions = sanctionService.getUserSanctions(userId);
        boolean isBanned = sanctionService.isBanned(userId);
        boolean isMuted = !sanctionService.canChat(userId);

        return ResponseEntity.ok(ApiResponse.ok(new UserDetail(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isAdmin(),
                user.getLevel(),
                user.getCoins(),
                isBanned,
                isMuted,
                sanctions
        )));
    }

    // DTOs에 추가
    public record UserInfo(
            String id,
            String username,
            String email,
            boolean admin,
            boolean banned,
            boolean muted
    ) {}

    public record UserDetail(
            String id,
            String username,
            String email,
            boolean admin,
            int level,
            int coins,
            boolean banned,
            boolean muted,
            List<Sanction> sanctions
    ) {}
    // ==================== DTOs ====================

    public record ReportDetail(
            Report report,
            String targetName,
            String reporterName,
            List<Sanction> targetSanctions
    ) {}

    public record ResolveRequest(
            ReportStatus status,
            String adminNote
    ) {}

    public record ResolveWithSanctionRequest(
            String adminNote,
            SanctionType sanctionType,
            String sanctionReason,
            Integer durationMinutes
    ) {}

    public record ApplySanctionRequest(
            String userId,
            SanctionType type,
            String reason,
            Integer durationMinutes
    ) {}
}