package com.achat.round.dto;

import com.achat.round.Round;
import com.achat.round.RoundPhase;
import com.achat.round.RoundScore;

import java.time.Instant;
import java.util.List;

public record RoundResponse(
        String roomId,
        RoundPhase phase,
        int currentPhaseNumber,
        int totalPhases,
        Instant phaseEndsAt,
        int durationMinutes,
        List<ScoreSnapshot> scores,
        String roundId,
        String gameId,
        Instant serverTime
) {
    public record ScoreSnapshot(
            String userId,
            int messageCount,
            int totalPoints,
            int rank
    ) {
        public static ScoreSnapshot from(RoundScore score) {
            return new ScoreSnapshot(
                    score.getUserId(),
                    score.getScoredMessageCount(),  // 수정
                    score.getTotalPoints(),
                    score.getRank()
            );
        }
    }

    public static RoundResponse from(Round round) {
        List<ScoreSnapshot> scoreList = round.getScores().values().stream()
                .map(ScoreSnapshot::from)
                .sorted((a, b) -> b.totalPoints() - a.totalPoints())
                .toList();

        return new RoundResponse(
                round.getRoomId(),
                round.getPhase(),
                round.getCurrentPhaseNumber(),
                3,
                round.getPhaseEndsAt(),
                round.getDurationMinutes(),
                scoreList, round.getRoundId(), round.getGameId(), Instant.now()
        );
    }

    public static RoundResponse waiting(String roomId) {
        return new RoundResponse(roomId, RoundPhase.WAITING, 0, 3, null, 0, List.of(), null, null, Instant.now());
    }
}
