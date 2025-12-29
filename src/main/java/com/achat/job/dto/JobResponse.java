package com.achat.job.dto;

import com.achat.job.JobDefinition;
import com.achat.job.JobType;

public record JobResponse(
        JobType type,
        String name,
        String description,
        String passiveDescription
) {
    public static JobResponse from(JobDefinition def) {
        return new JobResponse(def.type(), def.name(), def.description(), def.passiveDescription());
    }
}