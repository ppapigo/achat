package com.achat.message.dto;

import com.achat.message.Message;

import java.time.Instant;
import java.util.Set;
import java.util.List;
import com.achat.augment.AugmentType;
import com.achat.augment.effect.MessageEffect;

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
        String replyToPreview,
        String senderName,
        boolean anonymous,
        List<MessageEffect> augmentEffects,
        long revision,
        Instant serverTime,
        String roundId
) {
    public MessageResponse withEffects(List<MessageEffect> effects) {
        return new MessageResponse(id, roomId, senderId, senderTitle, content, appliedContent, appliedAugment,
                createdAt, likeCount, likedByUserIds, replyToId, replyToPreview, senderName, anonymous, effects, revision, serverTime, roundId);
    }
    public static MessageResponse from(Message message) {
        return from(message, null, message.getReplyToPreview());
    }

    public static MessageResponse from(Message message, String senderName, String replyPreview) {
        boolean anonymous = message.hasAugment(AugmentType.SHADOW_SPEAKER);
        Instant now = Instant.now();
        var effects = message.getAugmentEffects().stream().map(effect ->
                effect.type() == AugmentType.TIME_CAPSULE && effect.revealAt() != null && !now.isBefore(effect.revealAt())
                        ? effect.resolved("REVEALED", effect.points()) : effect).toList();
        boolean locked = effects.stream().anyMatch(effect -> effect.type() == AugmentType.TIME_CAPSULE
                && effect.revealAt() != null && now.isBefore(effect.revealAt()));
        return new MessageResponse(
                message.getId(),
                message.getRoomId(),
                anonymous ? null : message.getSenderId(),
                anonymous ? null : message.getSenderTitle(),
                locked ? null : message.getContent(),
                locked ? null : message.getAppliedContent(),
                message.getAppliedAugment(),
                message.getCreatedAt(),
                message.getLikeCount(),
                Set.copyOf(message.getLikedByUserIds()),
                message.getReplyToId(),
                replyPreview,
                anonymous ? "익명" : senderName,
                anonymous, effects, message.getRevision(), now, message.getRoundId()
        );
    }
}
