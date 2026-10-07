package com.achat.feed;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feed_comments", indexes = @Index(columnList = "post_id,created_at"))
public class FeedComment extends JpaEntity {
    protected FeedComment() {}

    @Id
    @Column(length = 36)
    private String id;
    @Column(length = 36)
    private String postId;
    @Column(length = 36)
    private String authorId;
    private String authorUsername;
    @Column(columnDefinition = "TEXT")
    private String content;
    private Instant createdAt;

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