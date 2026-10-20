package com.achat.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank @Email @jakarta.validation.constraints.Size(max = 254) String email,
        @NotBlank @jakarta.validation.constraints.Size(max = 100) String password
) {}
