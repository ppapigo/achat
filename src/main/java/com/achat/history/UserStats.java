package com.achat.history;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class UserStats {
    private int totalGames;
    private int wins;
    private int losses;
    private int draws;
    private double winRate;
    private double avgScore;
    private int bestScore;
    private int currentStreak;
}
