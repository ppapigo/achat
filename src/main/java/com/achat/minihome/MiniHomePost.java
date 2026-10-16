package com.achat.minihome;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "mini_home_posts", indexes = @Index(columnList = "author_id,created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class MiniHomePost extends JpaEntity {

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

    public void addLike() { this.likeCount++; }
    public void removeLike() { this.likeCount--; }
}
