package com.achat.user;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import com.achat.rank.Tier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends JpaEntity {

    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(nullable = false, length = 100)
    @Setter
    private String username;
    @Column(nullable = false, unique = true, length = 100)
    @Getter(AccessLevel.NONE)
    private String usernameKey;
    private String passwordHash;
    public void changePassword(String passwordHash) { this.passwordHash = passwordHash; }
    // Keep the legacy column writable for existing databases with a NOT NULL constraint.
    // It is no longer exposed or used for account access.
    @Getter(AccessLevel.NONE)
    @Column(name = "email_verified")
    private boolean legacyEmailVerified;
    private Instant createdAt;

    // 재화 시스템
    private int coins;
    private int exp;
    private int level;

    // 랭크 시스템
    private int rp;
    private int rankedGames;
    @Enumerated(EnumType.STRING)
    private Tier seasonHighestTier;
    private int seasonId;

    // 배치고사 시스템
    private boolean placementCompleted;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_placement_ranks", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "game_index")
    @Column(name = "finish_rank")
    private List<Integer> placementRanks;
    @Setter
    private boolean isAdmin = false;
    public static final int PLACEMENT_GAMES_REQUIRED = 5;

    public User(String email, String passwordHash) {
        this.id = UUID.randomUUID().toString();
        this.email = email.trim().toLowerCase(java.util.Locale.ROOT);
        this.username = generateDefaultUsername(email);  // 추가!
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();

        // 재화 초기값
        this.coins = 1000;
        this.exp = 0;
        this.level = 1;

        // 랭크 초기값
        this.rp = 0;
        this.rankedGames = 0;
        this.seasonHighestTier = Tier.UNRANKED;
        this.seasonId = 1;

        // 배치고사 초기값
        this.placementCompleted = false;
        this.placementRanks = new ArrayList<>();
    }

    // 이메일에서 기본 username 생성
    private static String generateDefaultUsername(String email) {
        String base = email.split("@")[0];
        // 4자리 랜덤 숫자 추가
        int random = (int) (Math.random() * 9000) + 1000;
        return base + "#" + random;
    }

    @PrePersist
    @PreUpdate
    private void normalizeIdentity() {
        email = email.trim().toLowerCase(java.util.Locale.ROOT);
        usernameKey = username.toLowerCase(java.util.Locale.ROOT);
    }

    public void addReward(int coins, int exp) {
        this.coins += coins;
        this.exp += exp;
        checkLevelUp();
    }

    public void addCoins(int amount) { this.coins += amount; }

    public boolean spendCoins(int amount) {
        if (this.coins >= amount) {
            this.coins -= amount;
            return true;
        }
        return false;
    }

    private void checkLevelUp() {
        int requiredExp = level * 100;
        while (this.exp >= requiredExp) {
            this.exp -= requiredExp;
            this.level++;
            requiredExp = level * 100;
        }
    }

    public int getExpToNextLevel() {
        return (level * 100) - exp;
    }

    // === 랭크 ===
    public Tier getTier() {
        if (!placementCompleted) {
            return Tier.UNRANKED;
        }
        return Tier.fromRp(rp);
    }

    // === 배치고사 ===
    public List<Integer> getPlacementRanks() { return new ArrayList<>(placementRanks); }
    public int getPlacementGamesPlayed() { return placementRanks.size(); }
    public int getPlacementGamesRemaining() {
        return Math.max(0, PLACEMENT_GAMES_REQUIRED - placementRanks.size());
    }

    public boolean recordPlacementGame(int rank) {
        if (placementCompleted) {
            return false;
        }

        placementRanks.add(rank);
        rankedGames++;

        if (placementRanks.size() >= PLACEMENT_GAMES_REQUIRED) {
            completePlacement();
            return true;
        }
        return false;
    }

    private void completePlacement() {
        double avgRank = placementRanks.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(3.0);

        int initialRp;
        if (avgRank <= 1.5) {
            initialRp = 800;
        } else if (avgRank <= 2.0) {
            initialRp = 600;
        } else if (avgRank <= 2.5) {
            initialRp = 400;
        } else if (avgRank <= 3.0) {
            initialRp = 200;
        } else {
            initialRp = 0;
        }

        this.rp = initialRp;
        this.placementCompleted = true;
        this.seasonHighestTier = getTier();

        System.out.println("배치 완료: " + email
                + " | 평균 순위: " + String.format("%.2f", avgRank)
                + " | 초기 RP: " + initialRp
                + " | 티어: " + getTier());
    }

    public int applyRpChange(int change) {
        if (!placementCompleted) {
            return 0;
        }

        int oldRp = this.rp;
        this.rp = Math.max(0, this.rp + change);

        Tier currentTier = getTier();
        if (currentTier.ordinal() > seasonHighestTier.ordinal()) {
            this.seasonHighestTier = currentTier;
        }

        return this.rp - oldRp;
    }

    public void resetSeason(int newSeasonId) {
        this.rp = (int) (this.rp * 0.3);
        this.seasonHighestTier = getTier();
        this.seasonId = newSeasonId;
    }

    public void resetSeasonFull(int newSeasonId) {
        this.rp = 0;
        this.rankedGames = 0;
        this.placementCompleted = false;
        this.placementRanks = new ArrayList<>();
        this.seasonHighestTier = Tier.UNRANKED;
        this.seasonId = newSeasonId;
    }

    public int getRpToNextTier() {
        if (!placementCompleted) return 0;
        return getTier().getRpToNext(rp);
    }

    public boolean isRanked() {
        return placementCompleted;
    }
}
