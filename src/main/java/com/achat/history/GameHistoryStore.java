package com.achat.history;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class GameHistoryStore {
    private final Map<String, List<GameHistory>> historyByUser = new ConcurrentHashMap<>();

    public void save(GameHistory history) {
        historyByUser.computeIfAbsent(history.getUserId(), k -> new ArrayList<>())
                .add(history);
    }

    public List<GameHistory> findByUserId(String odUserId) {
        return historyByUser.getOrDefault(odUserId, Collections.emptyList());
    }

    public List<GameHistory> findRecentByUserId(String odUserId, int limit) {
        List<GameHistory> all = findByUserId(odUserId);
        return all.stream()
                .sorted(Comparator.comparing(GameHistory::getPlayedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
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