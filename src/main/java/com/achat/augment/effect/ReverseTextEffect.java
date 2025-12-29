package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class ReverseTextEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.REVERSE_TEXT; }

    @Override
    public String getName() { return "리버스"; }

    @Override
    public String getDescription() { return "메시지가 거꾸로 표시됩니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return new StringBuilder(message).reverse().toString();
    }
}