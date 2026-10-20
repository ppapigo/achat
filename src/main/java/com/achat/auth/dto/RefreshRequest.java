package com.achat.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank @jakarta.validation.constraints.Size(max = 4096) String refreshToken
) {}
