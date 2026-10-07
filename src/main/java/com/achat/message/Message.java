package com.achat.message;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "chat_messages", indexes = @Index(columnList = "room_id,created_at"))
public class Message extends JpaEntity {
    protected Message() {}

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
    private String appliedAugment;
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "message_likes", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "user_id", length = 36)
    private Set<String> likedByUserIds;
    @Column(length = 36)
    private String replyToId;
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

    // Getters
    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getSenderId() { return senderId; }
    public String getSenderTitle() { return senderTitle; }
    public String getContent() { return content; }
    public String getAppliedContent() { return appliedContent; }
    public String getAppliedAugment() { return appliedAugment; }
    public Instant getCreatedAt() { return createdAt; }

    public Set<String> getLikedByUserIds() { return likedByUserIds; }
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

    public String getReplyToId() { return replyToId; }
    public String getReplyToPreview() { return replyToPreview; }
    public void setReplyToPreview(String preview) { this.replyToPreview = preview; }
    public boolean isReply() { return replyToId != null; }
}