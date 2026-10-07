package com.achat.board;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "boards", indexes = @Index(columnList = "hidden,category,created_at"))
public class Board extends JpaEntity {
    protected Board() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 36)
    private String authorId;
    private String authorNickname;
    @Enumerated(EnumType.STRING)
    private BoardCategory category;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private boolean hidden;  // ✅ 추가
    @Column(columnDefinition = "TEXT")
    private String hiddenReason;  // ✅ 추가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Board(Long id, String authorId, String authorNickname,
                 BoardCategory category, String title, String content) {
        this.id = id;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
        this.category = category;
        this.title = title;
        this.content = content;
        this.viewCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
        this.hidden = false;
        this.hiddenReason = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getAuthorNickname() { return authorNickname; }
    public BoardCategory getCategory() { return category; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public int getViewCount() { return viewCount; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public boolean isHidden() { return hidden; }  // ✅ 추가
    public String getHiddenReason() { return hiddenReason; }  // ✅ 추가
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // 수정
    public void update(BoardCategory category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ 숨김 처리
    public void hide(String reason) {
        this.hidden = true;
        this.hiddenReason = reason;
    }

    // ✅ 숨김 해제
    public void unhide() {
        this.hidden = false;
        this.hiddenReason = null;
    }

    public void incrementViewCount() { this.viewCount++; }
    public void incrementLikeCount() { this.likeCount++; }
    public void decrementLikeCount() { if (this.likeCount > 0) this.likeCount--; }
    public void incrementCommentCount() { this.commentCount++; }
    public void decrementCommentCount() { if (this.commentCount > 0) this.commentCount--; }
}