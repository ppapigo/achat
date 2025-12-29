package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentType;

public class EchoBurstEffect implements AugmentEffect {
    @Override
    public AugmentType getType() { return AugmentType.ECHO_BURST; }

    @Override
    public String getName() { return "에코 버스트"; }

    @Override
    public String getDescription() { return "첫 줄이 강조 표시됩니다"; }

    @Override
    public String apply(String message, AugmentContext context) {
        String[] lines = message.split("\n", 2);
        String firstLine = lines[0].toUpperCase();
        if (lines.length > 1) {
            return "📢 " + firstLine + "\n" + lines[1];
        }
        return "📢 " + firstLine;
    }
}