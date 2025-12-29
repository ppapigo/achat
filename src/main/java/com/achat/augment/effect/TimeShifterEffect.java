package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class TimeShifterEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.TIME_SHIFTER; }

    @Override
    public String getName() { return "타임 시프터"; }

    @Override
    public String getDescription() { return "관심 메시지가 상단에 표시됩니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return "⏱️ " + message;
    }
}