package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class SpotlightEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.SPOTLIGHT; }

    @Override
    public String getName() { return "스팟라이트"; }

    @Override
    public String getDescription() { return "메시지를 강조 고정합니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return "🔦 " + message.toUpperCase() + " 🔦";
    }
}