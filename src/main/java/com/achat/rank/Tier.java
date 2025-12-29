package com.achat.rank;

public enum Tier {
    UNRANKED(-1, -1, "언랭", "❓"),      // 추가!
    BRONZE(0, 399, "브론즈", "🥉"),
    SILVER(400, 799, "실버", "🥈"),
    GOLD(800, 1199, "골드", "🥇"),
    PLATINUM(1200, 1599, "플래티넘", "💎"),
    DIAMOND(1600, 1999, "다이아몬드", "💠"),
    CHALLENGER(2000, Integer.MAX_VALUE, "챌린저", "👑");

    private final int minRp;
    private final int maxRp;
    private final String displayName;
    private final String emoji;

    Tier(int minRp, int maxRp, String displayName, String emoji) {
        this.minRp = minRp;
        this.maxRp = maxRp;
        this.displayName = displayName;
        this.emoji = emoji;
    }

    public int getMinRp() { return minRp; }
    public int getMaxRp() { return maxRp; }
    public String getDisplayName() { return displayName; }
    public String getEmoji() { return emoji; }

    // RP로 티어 계산 (rankedGames 체크 필요)
    public static Tier fromRp(int rp, int rankedGames) {
        // 랭크 게임 0회면 UNRANKED
        if (rankedGames <= 0) {
            return UNRANKED;
        }
        return fromRp(rp);
    }

    // RP만으로 티어 계산 (기존 호환)
    public static Tier fromRp(int rp) {
        if (rp < 0) return UNRANKED;
        for (Tier tier : values()) {
            if (tier == UNRANKED) continue;
            if (rp >= tier.minRp && rp <= tier.maxRp) {
                return tier;
            }
        }
        return BRONZE;
    }

    // 다음 티어까지 필요 RP
    public int getRpToNext(int currentRp) {
        if (this == CHALLENGER || this == UNRANKED) return 0;
        return maxRp + 1 - currentRp;
    }

    // 다음 티어
    public Tier next() {
        if (this == UNRANKED) return BRONZE;
        int idx = this.ordinal();
        if (idx >= values().length - 1) return this;
        return values()[idx + 1];
    }

    // 배치 완료 여부
    public boolean isRanked() {
        return this != UNRANKED;
    }
}