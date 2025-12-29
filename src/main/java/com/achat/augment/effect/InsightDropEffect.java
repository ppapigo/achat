package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class InsightDropEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.INSIGHT_DROP; }

    @Override
    public String getName() { return "인사이트 드롭"; }

    @Override
    public String getDescription() { return "라운드 종료 시 하이라이트 제공"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return "💡 " + message;
    }
}