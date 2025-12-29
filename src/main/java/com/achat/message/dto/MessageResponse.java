package com.achat.message.dto;

import com.achat.message.Message;

import java.time.Instant;
import java.util.Set;

public record MessageResponse(
        String id,
        String roomId,
        String senderId,
        String senderTitle,  // 추가!
        String content,
        String appliedContent,
        String appliedAugment,
        Instant createdAt,
        int likeCount,
        Set<String> likedByUserIds,
        String replyToId,
        String replyToPreview
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                message.getSenderTitle(),  // 추가!
                message.getContent(),
                message.getAppliedContent(),
                message.getAppliedAugment(),
                message.getCreatedAt(),
                message.getLikeCount(),
                message.getLikedByUserIds(),
                message.getReplyToId(),
                message.getReplyToPreview()
        );
    }
}