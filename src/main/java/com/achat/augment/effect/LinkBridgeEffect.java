package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class LinkBridgeEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.LINK_BRIDGE; }

    @Override
    public String getName() { return "링크 브리지"; }

    @Override
    public String getDescription() { return "인용 메시지가 눈에 띄게 연결됩니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        return "🔗 " + message;
    }
}