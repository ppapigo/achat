package com.achat.message.dto;

public record LikeResponse(
        String messageId,
        int likeCount,
        boolean liked,
        String odUserId
) {}