package com.achat.job;

public record JobDefinition(
        JobType type,
        String name,
        String description,
        String passiveDescription
) {}