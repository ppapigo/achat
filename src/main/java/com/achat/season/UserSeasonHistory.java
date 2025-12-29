package com.achat.season;

import com.achat.rank.Tier;
import java.time.Instant;

public class UserSeasonHistory {
    private final String odUserId;  // 수정: userId → odUserId
    private final int seasonId;
    private final int finalRp;
    private final Tier finalTier;
    private final Tier highestTier;
    private final int rewardCoins;
    private final Instant recordedAt;

    public UserSeasonHistory(String odUserId, int seasonId, int finalRp,
                             Tier finalTier, Tier highestTier, int rewardCoins) {
        this.odUserId = odUserId;  // 수정!
        this.seasonId = seasonId;
        this.finalRp = finalRp;
        this.finalTier = finalTier;
        this.highestTier = highestTier;
        this.rewardCoins = rewardCoins;
        this.recordedAt = Instant.now();
    }

    public String getUserId() { return odUserId; }  // getter는 getUserId 유지
    public int getSeasonId() { return seasonId; }
    public int getFinalRp() { return finalRp; }
    public Tier getFinalTier() { return finalTier; }
    public Tier getHighestTier() { return highestTier; }
    public int getRewardCoins() { return rewardCoins; }
    public Instant getRecordedAt() { return recordedAt; }
}