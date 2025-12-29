package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

import java.util.Random;

public class VisualSpellEffect implements AugmentEffect {
    private static final String[] EFFECTS = {"✨", "💫", "🌟", "⭐", "🔮"};
    private final Random random = new Random();

    @Override
    public AugmentType getType() { return AugmentType.VISUAL_SPELL; }

    @Override
    public String getName() { return "비주얼 스펠"; }

    @Override
    public String getDescription() { return "메시지에 비주얼 이펙트 적용"; }

    @Override
    public String apply(String message, AugmentContext context) {
        String effect = EFFECTS[random.nextInt(EFFECTS.length)];
        if (message.length() <= 20) {
            return effect + " " + message + " " + effect;
        }
        return effect + " " + message;
    }
}