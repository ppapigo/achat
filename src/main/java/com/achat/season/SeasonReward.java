package com.achat.season;

import com.achat.rank.Tier;
import java.util.Map;

public class SeasonReward {

    // 티어별 시즌 종료 보상
    private static final Map<Tier, Integer> TIER_COINS = Map.of(
            Tier.UNRANKED, 50,    // 추가!
            Tier.BRONZE, 100,
            Tier.SILVER, 300,
            Tier.GOLD, 500,
            Tier.PLATINUM, 800,
            Tier.DIAMOND, 1200,
            Tier.CHALLENGER, 2000
    );

    // 시즌 최고 티어 보너스
    private static final Map<Tier, Integer> HIGHEST_TIER_BONUS = Map.of(
            Tier.UNRANKED, 0,     // 추가!
            Tier.BRONZE, 0,
            Tier.SILVER, 50,
            Tier.GOLD, 100,
            Tier.PLATINUM, 200,
            Tier.DIAMOND, 400,
            Tier.CHALLENGER, 800
    );

    public static int getRewardCoins(Tier finalTier, Tier highestTier) {
        int base = TIER_COINS.getOrDefault(finalTier, 50);
        int bonus = HIGHEST_TIER_BONUS.getOrDefault(highestTier, 0);
        return base + bonus;
    }

    public static int getRewardCoins(Tier tier) {
        return TIER_COINS.getOrDefault(tier, 50);
    }
}