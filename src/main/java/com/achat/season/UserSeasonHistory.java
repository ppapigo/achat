package com.achat.season;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import com.achat.rank.Tier;
import java.time.Instant;

@Entity
@Table(name = "user_season_histories", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "season_id"}))
public class UserSeasonHistory extends JpaEntity {
    protected UserSeasonHistory() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 36)
    private String userId;
    private int seasonId;
    private int finalRp;
    @Enumerated(EnumType.STRING)
    private Tier finalTier;
    @Enumerated(EnumType.STRING)
    private Tier highestTier;
    private int rewardCoins;
    private Instant recordedAt;

    public UserSeasonHistory(String userId, int seasonId, int finalRp,
                             Tier finalTier, Tier highestTier, int rewardCoins) {
        this.userId = userId;
        this.seasonId = seasonId;
        this.finalRp = finalRp;
        this.finalTier = finalTier;
        this.highestTier = highestTier;
        this.rewardCoins = rewardCoins;
        this.recordedAt = Instant.now();
    }

    public String getUserId() { return userId; }  // getter는 getUserId 유지
    public int getSeasonId() { return seasonId; }
    public int getFinalRp() { return finalRp; }
    public Tier getFinalTier() { return finalTier; }
    public Tier getHighestTier() { return highestTier; }
    public int getRewardCoins() { return rewardCoins; }
    public Instant getRecordedAt() { return recordedAt; }
}
