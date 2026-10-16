package com.achat.history;

import lombok.RequiredArgsConstructor;

import com.achat.common.ApiResponse;
import com.achat.history.dto.GameHistoryResponse;
import com.achat.history.dto.UserStatsResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/history")
@RequiredArgsConstructor
public class GameHistoryController {

    private final GameHistoryService historyService;

    // 내 전적 조회
    @GetMapping("/recent")
    public ApiResponse<Map<String, Object>> getMyHistory(
            @AuthenticationPrincipal String odUserId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<GameHistoryResponse> histories = historyService.getHistory(odUserId, limit);
        return ApiResponse.ok(Map.of(
                "histories", histories,
                "count", histories.size()
        ));
    }

    // 내 통계 조회
    @GetMapping("/stats")
    public ApiResponse<UserStatsResponse> getMyStats(@AuthenticationPrincipal String odUserId) {
        return ApiResponse.ok(historyService.getStats(odUserId));
    }

    // 전적 + 통계 요약 (한 번에)
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getSummary(@AuthenticationPrincipal String odUserId) {
        UserStatsResponse stats = historyService.getStats(odUserId);
        List<GameHistoryResponse> recent = historyService.getHistory(odUserId, 5);

        return ApiResponse.ok(Map.of(
                "stats", stats,
                "recentGames", recent
        ));
    }

    // 다른 유저 전적 조회
    @GetMapping("/user/{targetUserId}")
    public ApiResponse<List<GameHistoryResponse>> getUserHistory(
            @PathVariable String targetUserId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(historyService.getHistory(targetUserId, limit));
    }
}
