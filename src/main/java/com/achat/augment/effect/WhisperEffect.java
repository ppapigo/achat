package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class WhisperEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.WHISPER; }

    @Override
    public String getName() { return "속삭임"; }

    @Override
    public String getDescription() { return "모든 텍스트가 소문자로 변환됩니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return message.toLowerCase();
    }
}