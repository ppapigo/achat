package com.achat.minihome;

import java.time.LocalDateTime;

public class MiniHomePost {
    private String id;
    private String authorId;
    private String content;
    private int likeCount;
    private LocalDateTime createdAt;

    public MiniHomePost(String id, String authorId, String content) {
        this.id = id;
        this.authorId = authorId;
        this.content = content;
        this.likeCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public String getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public int getLikeCount() { return likeCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void addLike() { this.likeCount++; }
    public void removeLike() { this.likeCount--; }
}