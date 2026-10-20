package com.achat.message;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import com.achat.augment.AugmentType;
import com.achat.augment.effect.*;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(columnList = "room_id,created_at"), @Index(columnList = "next_effect_at")},
        uniqueConstraints = @UniqueConstraint(name = "uk_message_client_request",
                columnNames = {"room_id", "sender_id", "client_message_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Message extends JpaEntity {

    @Id
    @Column(length = 36)
    private String id;
    @Column(length = 36)
    private String roomId;
    @Column(length = 36)
    private String senderId;
    private String senderTitle;  // 추가!
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(columnDefinition = "TEXT")
    private String appliedContent;
    private String appliedAugment; // 적용된 증강 이름들 (여러 개면 쉼표로 구분)
    private Instant createdAt;
    @Setter
    private String roundId;
    @Setter
    private String gameId;
    @Setter @Column(length = 64)
    private String clientMessageId;
    @Convert(converter = MessageEffectsConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<MessageEffect> augmentEffects = List.of();
    private Long revision = 0L;
    @Setter
    private Instant nextEffectAt;
    @Setter
    private Instant highlightedAt;
    @Setter
    private Boolean rewardEligible = false;

    public boolean isRewardEligible() { return Boolean.TRUE.equals(rewardEligible); }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_likes", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id", length = 36)
    private Set<String> likedByUserIds;
    @Column(length = 36)
    private String replyToId;
    @Setter
    private String replyToPreview;

    public Message(String roomId, String senderId, String senderTitle, String content,
                   String appliedContent, String appliedAugment) {
        this(roomId, senderId, senderTitle, content, appliedContent, appliedAugment, null);
    }

    public Message(String roomId, String senderId, String senderTitle, String content,
                   String appliedContent, String appliedAugment, String replyToId) {
        this.id = UUID.randomUUID().toString();
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderTitle = senderTitle;
        this.content = content;
        this.appliedContent = appliedContent;
        this.appliedAugment = appliedAugment;
        this.createdAt = Instant.now();
        this.likedByUserIds = new HashSet<>();
        this.replyToId = replyToId;
    }

    public int getLikeCount() { return likedByUserIds.size(); }

    public boolean toggleLike(String userId) {
        revision = getRevision() + 1;
        if (likedByUserIds.contains(userId)) {
            likedByUserIds.remove(userId);
            return false;
        } else {
            likedByUserIds.add(userId);
            return true;
        }
    }

    public boolean isLikedBy(String userId) {
        return likedByUserIds.contains(userId);
    }

    public boolean isReply() { return replyToId != null; }

    public long getRevision() { return revision == null ? 0 : revision; }
    public List<MessageEffect> getAugmentEffects() { return augmentEffects == null ? List.of() : List.copyOf(augmentEffects); }
    public void setAugmentEffects(List<MessageEffect> effects) {
        augmentEffects = List.copyOf(effects);
        revision = getRevision() + 1;
    }
    public boolean hasAugment(AugmentType type) {
        return getAugmentEffects().stream().anyMatch(effect -> effect.type() == type)
                || (appliedAugment != null && List.of(appliedAugment.split(",")).contains(type.name()));
    }
}
