package com.achat.job.dto;

import jakarta.validation.constraints.NotBlank;

public record SelectJobRequest(
        @NotBlank String jobType
) {}