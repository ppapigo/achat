package com.achat.message;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Message {
    private final String id;
    private final String roomId;
    private final String senderId;
    private final String senderTitle;  // 추가!
    private final String content;
    private final String appliedContent;
    private final String appliedAugment;
    private final Instant createdAt;

    private final Set<String> likedByUserIds;
    private final String replyToId;
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