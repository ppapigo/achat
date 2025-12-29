package com.achat.augment.dto;

import com.achat.augment.AugmentDefinition;
import com.achat.augment.AugmentType;

import java.util.List;

public record AugmentOptionResponse(
        List<AugmentDefinition> options,
        AugmentType selected
) {}