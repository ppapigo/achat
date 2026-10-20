package com.achat.round.dto;

import com.achat.rank.RankService;
import com.achat.round.Round;
import com.achat.round.RoundReward;

import java.util.List;
import java.util.Map;
import com.achat.message.dto.MessageResponse;

public record RoundEndResponse(
        String roomId,
        List<PlayerResult> results,
        List<MessageResponse> highlights
) {
    public record PlayerResult(
            String odUserId,
            int rank,
            int messageCount,
            boolean augmentUsed,
            int totalPoints,
            int coinsEarned,
            int expEarned,
            // 랭크 정보
            int rpChange,
            int newRp,
            String oldTier,
            String newTier,
            String tierChange,
            // 배치 정보 (추가!)
            boolean inPlacement,
            int placementGamesPlayed,
            int placementGamesRemaining,
            boolean placementJustCompleted
    ) {}

    public static RoundEndResponse from(Round round, Map<String, RankService.RpResult> rpResults) {
        List<PlayerResult> results = round.getScores().values().stream()
                .map(score -> {
                    RoundReward reward = round.getRewards().get(score.getUserId());
                    RankService.RpResult rpResult = rpResults.get(score.getUserId());

                    return new PlayerResult(
                            score.getUserId(),
                            score.getRank(),
                            score.getScoredMessageCount(),
                            score.isAugmentUsed(),
                            score.getTotalPoints(),
                            reward != null ? reward.getCoins() : 0,
                            reward != null ? reward.getExp() : 0,
                            // 랭크 정보
                            rpResult != null ? rpResult.rpChange() : 0,
                            rpResult != null ? rpResult.newRp() : 0,
                            rpResult != null ? rpResult.oldTier().name() : "UNRANKED",
                            rpResult != null ? rpResult.newTier().name() : "UNRANKED",
                            rpResult != null ? rpResult.tierChange().name() : "NONE",
                            // 배치 정보
                            rpResult != null && rpResult.inPlacement(),
                            rpResult != null ? rpResult.placementGamesPlayed() : 0,
                            rpResult != null ? rpResult.placementGamesRemaining() : 5,
                            rpResult != null && rpResult.placementJustCompleted()
                    );
                })
                .sorted((a, b) -> a.rank() - b.rank())
                .toList();

        return new RoundEndResponse(round.getRoomId(), results, List.of());
    }

    public static RoundEndResponse from(Round round) {
        return from(round, Map.of());
    }
}
