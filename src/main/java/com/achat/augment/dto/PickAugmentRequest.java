package com.achat.augment.dto;

import com.achat.augment.AugmentType;
import jakarta.validation.constraints.NotNull;

public record PickAugmentRequest(
        @NotNull AugmentType type
) {}