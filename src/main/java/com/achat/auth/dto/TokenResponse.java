package com.achat.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String userId,
        String email
) {}