package com.achat.round.dto;

import jakarta.validation.constraints.NotNull;

public record StartRoundRequest(
        @NotNull Integer durationMinutes
) {
    public boolean isValidDuration() {
        return durationMinutes == 10 || durationMinutes == 15
                || durationMinutes == 20 || durationMinutes == 30;
    }
}