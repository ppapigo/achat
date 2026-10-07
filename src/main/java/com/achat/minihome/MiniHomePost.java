package com.achat.minihome;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "mini_home_posts", indexes = @Index(columnList = "author_id,created_at"))
public class MiniHomePost extends JpaEntity {
    protected MiniHomePost() {}

    @Id
    @Column(length = 36)
    private String id;
    @Column(length = 36)
    private String authorId;
    @Column(columnDefinition = "TEXT")
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