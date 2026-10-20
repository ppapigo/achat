package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class CapsLockEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.CAPS_LOCK; }

    @Override
    public String getName() { return "캡스락"; }

    @Override
    public String getDescription() { return "모든 텍스트가 대문자로 변환됩니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return message.toUpperCase(java.util.Locale.ROOT);
    }
}