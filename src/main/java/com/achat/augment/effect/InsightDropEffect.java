package com.achat.augment.effect;

import com.achat.augment.*;

/** Text is preserved; structured metadata drives the JSX presentation and server rules. */
public class InsightDropEffect implements AugmentEffect {
    public AugmentType getType() { return AugmentType.INSIGHT_DROP; }
    public String getName() { return AugmentDefinition.of(getType()).name(); }
    public String getDescription() { return AugmentDefinition.of(getType()).description(); }
    public String apply(String message, AugmentContext context) { return message; }
}
