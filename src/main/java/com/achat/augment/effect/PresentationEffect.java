package com.achat.augment.effect;

import com.achat.augment.*;

/** Presentation belongs to JSX, not prefixes or HTML injected into chat content. */
public record PresentationEffect(AugmentType type) implements AugmentEffect {
    public AugmentType getType() { return type; }
    public String getName() { return AugmentDefinition.of(type).name(); }
    public String getDescription() { return AugmentDefinition.of(type).description(); }
    public String apply(String message, AugmentContext context) { return message; }
}
