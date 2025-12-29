package com.achat.board;

import java.time.LocalDateTime;

public class Board {
    private Long id;
    private String authorId;
    private String authorNickname;
    private BoardCategory category;
    private String title;
    private String content;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private boolean hidden;  // ✅ 추가
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