package com.achat.room.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSubRoomRequest(
        @NotBlank String name
) {}