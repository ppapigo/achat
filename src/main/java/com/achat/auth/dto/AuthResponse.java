package com.achat.auth.dto;

public record AuthResponse(
        String userId,
        String email,
        String message
) {}
