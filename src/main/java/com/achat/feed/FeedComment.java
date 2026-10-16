package com.achat.feed;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feed_comments", indexes = @Index(columnList = "post_id,created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FeedComment extends JpaEntity {

    @Id
    @Column(length = 36)
    private String id;
    @Column(length = 36)
    private String postId;
    @Column(length = 36)
    private String authorId;
    @Setter
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
}
