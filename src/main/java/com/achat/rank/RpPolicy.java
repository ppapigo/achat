package com.achat.rank;

import java.util.Map;

/**
 * RP 증감 정책
 * 나중에 시즌/이벤트별로 다르게 적용 가능
 */
public class RpPolicy {

    // 순위별 RP 변동
    private static final Map<Integer, Integer> RANK_RP = Map.of(
            1, 30,
            2, 15,
            3, 5
    );

    // 4등 이하 기본 감소
    private static final int DEFAULT_LOSS = -10;

    // 최소 RP (음수 방지)
    public static final int MIN_RP = 0;

    /**
     * 순위에 따른 RP 변동량 계산
     */
    public static int calculateRpChange(int rank) {
        return RANK_RP.getOrDefault(rank, DEFAULT_LOSS);
    }

    /**
     * RP 적용 (최소값 보장)
     */
    public static int applyRpChange(int currentRp, int change) {
        return Math.max(MIN_RP, currentRp + change);
    }

    /**
     * 티어 승급/강등 체크
     */
    public static TierChange checkTierChange(Tier oldTier, Tier newTier) {
        if (newTier.ordinal() > oldTier.ordinal()) {
            return TierChange.PROMOTED;
        } else if (newTier.ordinal() < oldTier.ordinal()) {
            return TierChange.DEMOTED;
        }
        return TierChange.NONE;
    }

    public enum TierChange {
        NONE, PROMOTED, DEMOTED
    }
}