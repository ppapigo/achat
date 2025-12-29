package com.achat.history.dto;

import com.achat.history.UserStats;  // GameStats → UserStats로 변경!

public record UserStatsResponse(
        int totalGames,
        int wins,
        int losses,
        int draws,
        double winRate,
        double avgScore,
        int bestScore,
        int currentStreak,
        String streakText
) {
    public static UserStatsResponse from(UserStats stats) {  // GameStats → UserStats
        String streakText = "";
        int streak = stats.getCurrentStreak();
        if (streak > 0) {
            streakText = streak + "연승 🔥";
        } else if (streak < 0) {
            streakText = Math.abs(streak) + "연패 💀";
        }

        return new UserStatsResponse(
                stats.getTotalGames(),
                stats.getWins(),
                stats.getLosses(),
                stats.getDraws(),
                Math.round(stats.getWinRate() * 10) / 10.0,
                Math.round(stats.getAvgScore() * 10) / 10.0,
                stats.getBestScore(),
                stats.getCurrentStreak(),
                streakText
        );
    }
}