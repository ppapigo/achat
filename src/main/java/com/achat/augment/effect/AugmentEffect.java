package com.achat.augment.effect;

import com.achat.augment.AugmentContext;
import com.achat.augment.AugmentDefinition;
import com.achat.augment.AugmentType;

public interface AugmentEffect {

    AugmentType getType();
    String getName();
    String getDescription();

    String apply(String message, AugmentContext context);

    default AugmentDefinition toDefinition() {
        return new AugmentDefinition(getType(), getName(), getDescription());
    }
}