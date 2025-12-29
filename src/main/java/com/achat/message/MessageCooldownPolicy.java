package com.achat.message;

public class MessageCooldownPolicy {

    // 길이별 쿨타임 (밀리초)
    public static long getCooldownMs(int length) {
        if (length <= 40) return 0;           // 짧은 반응: 자유
        if (length <= 120) return 1000;       // 일반: 1초
        if (length <= 250) return 3000;       // 장문: 3초
        return 6000;                          // 초장문: 6초
    }

    // 장문 기준
    public static final int LONG_MESSAGE_THRESHOLD = 120;

    // 연속 장문 가중치
    public static double getConsecutiveMultiplier(int recentLongCount) {
        if (recentLongCount <= 1) return 1.0;
        if (recentLongCount == 2) return 1.5;
        return 2.0;
    }

    // 최대 쿨타임 (10초)
    public static final long MAX_COOLDOWN_MS = 10000;

    // 연속 체크 윈도우 (10초)
    public static final long CONSECUTIVE_WINDOW_MS = 10000;
}