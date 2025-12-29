package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class ThreadWeaverEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.THREAD_WEAVER; }

    @Override
    public String getName() { return "쓰레드 위버"; }

    @Override
    public String getDescription() { return "답장이 시각적으로 연결됩니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return "🧵 " + message;
    }
}