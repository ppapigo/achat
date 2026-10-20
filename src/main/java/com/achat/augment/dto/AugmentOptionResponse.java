package com.achat.augment.dto;

import com.achat.augment.AugmentDefinition;
import com.achat.augment.AugmentType;

import java.util.List;
import java.time.Instant;

public record AugmentOptionResponse(
        List<AugmentDefinition> options,
        AugmentType selected,
        String targetUserId,
        List<Target> targets,
        String roundId,
        Instant selectionEndsAt,
        Instant serverTime,
        boolean canSelect,
        AugmentType receivedAugment,
        String forbiddenWord
) {
    public record Target(String userId, String username, boolean available) {}
}
