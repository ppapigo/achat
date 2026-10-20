package com.achat.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateRoomRequest(
        @NotBlank @jakarta.validation.constraints.Size(max = 100) String name,
        @Min(2) @Max(12) int maxMembers
) {}
