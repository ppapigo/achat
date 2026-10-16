package com.achat.rank;

import lombok.RequiredArgsConstructor;

import com.achat.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    // 내 랭크 정보
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<RankService.RankInfo>> getMyRank(
            @AuthenticationPrincipal String userId) {
        RankService.RankInfo info = rankService.getRankInfo(userId);
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    // 리더보드 (상위 N명)
    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLeaderboard(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "20") int limit) {

        List<RankService.LeaderboardEntry> entries = rankService.getLeaderboard(Math.min(limit, 100));
        int myRank = rankService.getMyRank(userId);

        return ResponseEntity.ok(ApiResponse.ok(new LeaderboardResponse(entries, myRank)));
    }

    public record LeaderboardResponse(
            List<RankService.LeaderboardEntry> leaderboard,
            int myRank
    ) {}
}
