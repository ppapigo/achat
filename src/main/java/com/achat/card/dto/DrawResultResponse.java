package com.achat.card.dto;

public record DrawResultResponse(
        boolean success,
        boolean alreadyDrawnToday,
        int remainingDraws,
        ProfileCardResponse card
) {
    public static DrawResultResponse alreadyDrawn() {
        return new DrawResultResponse(false, true, 0, null);
    }

    public static DrawResultResponse noUsersAvailable() {
        return new DrawResultResponse(false, false, 1, null);
    }

    public static DrawResultResponse success(ProfileCardResponse card) {
        return new DrawResultResponse(true, false, 0, card);
    }
}