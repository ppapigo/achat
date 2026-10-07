package com.achat.history;

import org.springframework.stereotype.Component;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@RequiredArgsConstructor
public class GameHistoryStore {
    private final GameHistoryRepository repository;

    public void save(GameHistory history) { repository.save(history); }

    public List<GameHistory> findByUserId(String userId) { return repository.findByUserIdOrderByPlayedAtAsc(userId); }

    public List<GameHistory> findRecentByUserId(String odUserId, int limit) {
        return limit <= 0 ? List.of() : repository.findByUserIdOrderByPlayedAtDesc(odUserId, org.springframework.data.domain.PageRequest.of(0, limit));
    }

    public UserStats calculateStats(String odUserId) {
        List<GameHistory> histories = findByUserId(odUserId);
        UserStats stats = new UserStats();

        if (histories.isEmpty()) {
            return stats;
        }

        int totalGames = histories.size();
        int wins = 0;
        int topHalfCount = 0;
        int totalScore = 0;
        int bestScore = 0;

        for (GameHistory h : histories) {
            totalScore += h.getTotalPoints();
            bestScore = Math.max(bestScore, h.getTotalPoints());

            if (h.isWin()) {
                wins++;
            }
            if (h.isTopHalf()) {
                topHalfCount++;
            }
        }

        // 연승/연패 계산 (1등 기준)
        List<GameHistory> sorted = histories.stream()
                .sorted(Comparator.comparing(GameHistory::getPlayedAt).reversed())
                .toList();

        int streak = 0;
        if (!sorted.isEmpty()) {
            boolean firstIsWin = sorted.get(0).isWin();
            for (GameHistory h : sorted) {
                if (h.isWin() == firstIsWin) {
                    streak++;
                } else {
                    break;
                }
            }
            if (!firstIsWin) {
                streak = -streak;
            }
        }

        stats.setTotalGames(totalGames);
        stats.setWins(wins);
        stats.setLosses(totalGames - wins);
        stats.setDraws(0);  // 방 기반이라 무승부 없음
        stats.setWinRate(totalGames > 0 ? (double) wins / totalGames * 100 : 0);
        stats.setAvgScore(totalGames > 0 ? (double) totalScore / totalGames : 0);
        stats.setBestScore(bestScore);
        stats.setCurrentStreak(streak);

        return stats;
    }
}