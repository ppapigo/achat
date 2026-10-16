package com.achat.rank;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.round.Round;
import com.achat.round.RoundScore;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class RankService {

    private final UserStore userStore;

    /**
     * 라운드 종료 시 RP 적용 (배치 로직 포함)
     */
    public Map<String, RpResult> applyRoundResults(Round round) {
        Map<String, RpResult> results = new HashMap<>();

        for (RoundScore score : round.getScores().values()) {
            String odUserId = score.getUserId();
            int rank = score.getRank();

            userStore.findById(odUserId).ifPresent(user -> {

                Tier oldTier = user.getTier();
                int rpChange = 0;
                boolean placementJustCompleted = false;

                if (!user.isPlacementCompleted()) {
                    // 배치 중 - 순위 기록

                    placementJustCompleted = user.recordPlacementGame(rank);

                    if (placementJustCompleted) {
                        // 배치 완료됨 - 초기 RP가 세팅됨
                        rpChange = user.getRp();  // 초기 RP를 변동량으로 표시
                    }
                } else {
                    // 배치 완료 - 일반 RP 증감
                    rpChange = RpPolicy.calculateRpChange(rank);
                    user.applyRpChange(rpChange);
                }

                userStore.save(user);
                Tier newTier = user.getTier();
                RpPolicy.TierChange tierChange = RpPolicy.checkTierChange(oldTier, newTier);

                results.put(odUserId, new RpResult(
                        odUserId,
                        rpChange,
                        user.getRp(),
                        oldTier,
                        newTier,
                        tierChange,
                        !user.isPlacementCompleted(),  // 배치 중 여부
                        user.getPlacementGamesPlayed(),
                        user.getPlacementGamesRemaining(),
                        placementJustCompleted
                ));
            });
        }

        return results;
    }

    /**
     * 유저 랭크 정보 조회
     */
    public RankInfo getRankInfo(String odUserId) {
        User user = userStore.findById(odUserId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다"));

        int myRank = user.isPlacementCompleted() ? getMyRank(odUserId) : -1;

        return new RankInfo(
                user.getId(),
                user.getRp(),
                user.getTier(),
                user.getSeasonHighestTier(),
                user.getRpToNextTier(),
                user.getSeasonId(),
                myRank,
                user.isPlacementCompleted(),
                user.getPlacementGamesPlayed(),
                user.getPlacementGamesRemaining()
        );
    }

    /**
     * 리더보드 조회 (배치 완료된 유저만)
     */
    public List<LeaderboardEntry> getLeaderboard(int limit) {
        List<User> sorted = userStore.findAll().stream()
                .filter(User::isPlacementCompleted)  // 배치 완료된 유저만
                .sorted((a, b) -> b.getRp() - a.getRp())
                .limit(limit)
                .toList();

        List<LeaderboardEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            User user = sorted.get(i);
            entries.add(new LeaderboardEntry(
                    i + 1,
                    user.getId(),
                    maskEmail(user.getEmail()),
                    user.getRp(),
                    user.getTier(),
                    user.getLevel()
            ));
        }
        return entries;
    }

    /**
     * 내 순위 조회 (배치 완료된 유저 중)
     */
    public int getMyRank(String odUserId) {
        List<User> sorted = userStore.findAll().stream()
                .filter(User::isPlacementCompleted)
                .sorted((a, b) -> b.getRp() - a.getRp())
                .toList();

        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(odUserId)) {
                return i + 1;
            }
        }
        return -1;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String name = parts[0];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + parts[1];
        }
        return name.substring(0, 2) + "***@" + parts[1];
    }

    // === Records ===

    public record RpResult(
            String odUserId,
            int rpChange,
            int newRp,
            Tier oldTier,
            Tier newTier,
            RpPolicy.TierChange tierChange,
            boolean inPlacement,          // 배치 중 여부
            int placementGamesPlayed,     // 배치전 플레이 수
            int placementGamesRemaining,  // 남은 배치전
            boolean placementJustCompleted // 이번 판에 배치 완료됨
    ) {}

    public record RankInfo(
            String odUserId,
            int rp,
            Tier tier,
            Tier seasonHighestTier,
            int rpToNextTier,
            int seasonId,
            int rank,
            boolean placementCompleted,
            int placementGamesPlayed,
            int placementGamesRemaining
    ) {}

    public record LeaderboardEntry(
            int rank,
            String odUserId,
            String displayName,
            int rp,
            Tier tier,
            int level
    ) {}
}
