package com.achat.auth.dto;

public record AuthResponse(
        String userId,
        String email,
        boolean emailVerified,
        String message
) {}