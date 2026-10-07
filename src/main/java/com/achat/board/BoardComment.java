package com.achat.board;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "board_comments", indexes = @Index(columnList = "board_id,created_at"))
public class BoardComment extends JpaEntity {
    protected BoardComment() {}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long boardId;
    private Long parentId;
    @Column(length = 36)
    private String authorId;
    private String authorNickname;
    @Column(columnDefinition = "TEXT")
    private String content;
    private boolean hidden;  // ✅ 추가
    @Column(columnDefinition = "TEXT")
    private String hiddenReason;  // ✅ 추가
    private LocalDateTime createdAt;

    // 일반 댓글 생성자
    public BoardComment(Long id, Long boardId, String authorId,
                        String authorNickname, String content) {
        this(id, boardId, null, authorId, authorNickname, content);
    }

    // 대댓글 생성자
    public BoardComment(Long id, Long boardId, Long parentId, String authorId,
                        String authorNickname, String content) {
        this.id = id;
        this.boardId = boardId;
        this.parentId = parentId;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
        this.content = content;
        this.hidden = false;
        this.hiddenReason = null;
        this.createdAt = LocalDateTime.now();
    }

    // 내용 수정
    public void updateContent(String content) {
        this.content = content;
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

    public Long getId() { return id; }
    public Long getBoardId() { return boardId; }
    public Long getParentId() { return parentId; }
    public String getAuthorId() { return authorId; }
    public String getAuthorNickname() { return authorNickname; }
    public String getContent() { return content; }
    public boolean isHidden() { return hidden; }  // ✅ 추가
    public String getHiddenReason() { return hiddenReason; }  // ✅ 추가
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isReply() { return parentId != null; }
}