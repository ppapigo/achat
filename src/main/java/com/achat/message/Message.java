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

@Entity
@Table(name = "chat_messages", indexes = @Index(columnList = "room_id,created_at"))
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
}
