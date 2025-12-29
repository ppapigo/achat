package com.achat.history.dto;

import com.achat.history.GameHistory;

import java.time.Instant;

public record GameHistoryResponse(
        String id,
        String roomName,
        boolean isMatchRoom,
        int playerCount,
        int rank,
        int totalPoints,
        int messageCount,
        int rpChange,
        int coinsEarned,
        int expEarned,
        String augmentUsed,
        boolean isWin,
        Instant playedAt
) {
    public static GameHistoryResponse from(GameHistory h) {
        return new GameHistoryResponse(
                h.getId(),
                h.getRoomName(),
                h.isMatchRoom(),
                h.getPlayerCount(),
                h.getRank(),
                h.getTotalPoints(),
                h.getMessageCount(),
                h.getRpChange(),
                h.getCoinsEarned(),
                h.getExpEarned(),
                h.getAugmentUsed(),
                h.isWin(),
                h.getPlayedAt()
        );
    }
}