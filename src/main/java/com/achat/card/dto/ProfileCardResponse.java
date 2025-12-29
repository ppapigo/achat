package com.achat.card.dto;

import com.achat.user.User;

public record ProfileCardResponse(
        Long cardId,
        String ownerId,
        String targetId,
        String username,
        int level,
        String tier,
        int rp,
        boolean isOnline,
        boolean isFriend,
        String drawnDate
) {
    public static ProfileCardResponse from(Long cardId, User user, boolean isOnline, boolean isFriend, String drawnDate) {
        return new ProfileCardResponse(
                cardId,
                null,  // ownerId는 응답에서 필요 없음
                user.getId(),
                user.getUsername(),
                user.getLevel(),
                user.getTier().name(),
                user.getRp(),
                isOnline,
                isFriend,
                drawnDate
        );
    }
}