package com.achat.augment;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class AugmentPolicy {
    public static final int CAPSULE_SECONDS = 10;
    public static final int BOMB_SECONDS = 15;
    public static final int BOMB_POINTS = 10;
    public static final int JACKPOT_PERCENT = 10;
    public static final int JACKPOT_POINTS = 10;
    public static final int MAX_DRAWS = 10;
    public static final int MAX_JACKPOT_WINS = 3;
    public static final int MAX_EFFECT_POINTS = 50;
    public static final int FORBIDDEN_MESSAGES = 5;
    public static final int FORBIDDEN_PENALTY = 3;
    public static final int FORBIDDEN_BONUS = 15;
    public static final int LIKE_THRESHOLD = 5;
    public static final int LIKE_POINTS = 10;
    public static final int LIKE_MAX_REWARDS = 3;
    public static final int MAIN_CHARACTER_SECONDS = 6;
    public static final List<String> SAFE_WORDS = List.of("바나나", "무지개", "우산", "별빛", "hello");

    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Z}\\p{Cf}]+", "");
    }
    private AugmentPolicy() {}
}
