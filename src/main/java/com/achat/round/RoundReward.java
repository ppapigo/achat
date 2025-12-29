package com.achat.round;

import java.util.Map;

public class RoundReward {
    private final int rank;
    private final int coins;
    private final int exp;

    public RoundReward(int rank, int coins, int exp) {
        this.rank = rank;
        this.coins = coins;
        this.exp = exp;
    }

    // 순위별 보상 테이블
    private static final Map<Integer, RoundReward> REWARDS = Map.of(
            1, new RoundReward(1, 100, 50),
            2, new RoundReward(2, 70, 35),
            3, new RoundReward(3, 50, 25),
            4, new RoundReward(4, 30, 15)
    );

    private static final RoundReward PARTICIPATION = new RoundReward(0, 10, 5);

    public static RoundReward forRank(int rank) {
        return REWARDS.getOrDefault(rank, PARTICIPATION);
    }

    public int getRank() { return rank; }
    public int getCoins() { return coins; }
    public int getExp() { return exp; }
}