package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class TurboCasterEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.TURBO_CASTER; }

    @Override
    public String getName() { return "터보 캐스터"; }

    @Override
    public String getDescription() { return "메시지 쿨타임 40% 감소"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return message + " ⚡";
    }
}