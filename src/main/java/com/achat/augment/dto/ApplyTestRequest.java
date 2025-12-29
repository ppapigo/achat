package com.achat.augment.dto;

import jakarta.validation.constraints.NotBlank;

public record ApplyTestRequest(
        @NotBlank String message
) {}