package com.achat.post;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Post {

    private final String id;
    private final String authorId;
    private final PostType type;
    private String content;
    private List<String> tags;
    private long likeCount;
    private final Instant createdAt;
    private Instant updatedAt;

    public Post(String authorId, PostType type, String content) {
        this.id = UUID.randomUUID().toString();
        this.authorId = authorId;
        this.type = type;
        setContent(content);
        this.tags = new ArrayList<>();
        this.likeCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // === Getters ===
    public String getId() { return id; }
    public String getAuthorId() { return authorId; }
    public PostType getType() { return type; }
    public String getContent() { return content; }
    public List<String> getTags() { return new ArrayList<>(tags); }
    public long getLikeCount() { return likeCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // === Update 메서드 ===
    public void setContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용은 필수입니다");
        }
        if (content.length() > 500) {
            throw new IllegalArgumentException("내용은 500자 이하입니다");
        }
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public void setTags(List<String> tags) {
        if (tags != null && tags.size() > 3) {
            throw new IllegalArgumentException("태그는 최대 3개입니다");
        }
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    public void incrementLike() {
        this.likeCount++;
    }

    public void decrementLike() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}