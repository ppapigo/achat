package com.achat.feed;

import java.time.Instant;
import java.util.UUID;

public class FeedComment {
    private final String id;
    private final String postId;
    private final String authorId;
    private String authorUsername;
    private final String content;
    private final Instant createdAt;

    public FeedComment(String postId, String authorId, String content) {
        this.id = UUID.randomUUID().toString();
        this.postId = postId;
        this.authorId = authorId;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getPostId() { return postId; }
    public String getAuthorId() { return authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String username) { this.authorUsername = username; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}