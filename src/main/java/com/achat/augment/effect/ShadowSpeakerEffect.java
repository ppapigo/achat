package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class ShadowSpeakerEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.SHADOW_SPEAKER; }

    @Override
    public String getName() { return "그림자 화자"; }

    @Override
    public String getDescription() { return "익명으로 발언합니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return "👤 " + message;
    }
}