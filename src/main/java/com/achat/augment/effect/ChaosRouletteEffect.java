package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

import java.util.Random;

public class ChaosRouletteEffect implements AugmentEffect {

    private static final String[] RANDOM_EMOJIS = {
            "🎲", "🔥", "💀", "🤡", "👻", "🎪", "🌪️", "⚡",
            "🦄", "🍕", "🚀", "💣", "🎭", "🌈", "🎯", "🃏",
            "👽", "🤖", "🦑", "🐙", "🦖", "🌶️", "🍄", "💎"
    };

    private static final String[] CHAOS_EFFECTS = {
            "REVERSE",      // 글자 뒤집기
            "EMOJI_SPAM",   // 이모지 폭탄
            "CAPS",         // 대문자
            "STUTTER",      // 말더듬
            "NORMAL"        // 그냥 이모지만
    };

    private final Random random = new Random();

    @Override
    public AugmentType getType() { return AugmentType.CHAOS_ROULETTE; }

    @Override
    public String getName() { return "카오스 룰렛"; }

    @Override
    public String getDescription() { return "메시지에 랜덤 효과가 적용됩니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        String effect = CHAOS_EFFECTS[random.nextInt(CHAOS_EFFECTS.length)];
        String emoji = RANDOM_EMOJIS[random.nextInt(RANDOM_EMOJIS.length)];

        return switch (effect) {
            case "REVERSE" -> emoji + " " + new StringBuilder(message).reverse().toString();
            case "EMOJI_SPAM" -> {
                String e1 = RANDOM_EMOJIS[random.nextInt(RANDOM_EMOJIS.length)];
                String e2 = RANDOM_EMOJIS[random.nextInt(RANDOM_EMOJIS.length)];
                String e3 = RANDOM_EMOJIS[random.nextInt(RANDOM_EMOJIS.length)];
                yield e1 + e2 + e3 + " " + message + " " + e3 + e2 + e1;
            }
            case "CAPS" -> emoji + " " + message.toUpperCase() + "!!!";
            case "STUTTER" -> {
                if (message.length() > 0) {
                    String first = message.substring(0, Math.min(2, message.length()));
                    yield emoji + " " + first + "..." + first + "..." + message;
                }
                yield emoji + " " + message;
            }
            default -> emoji + " " + message + " " + emoji;
        };
    }
}