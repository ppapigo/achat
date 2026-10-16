package com.achat.season;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import com.achat.rank.Tier;
import java.time.Instant;

@Entity
@Table(name = "user_season_histories", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "season_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserSeasonHistory extends JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter(AccessLevel.NONE)
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
}
