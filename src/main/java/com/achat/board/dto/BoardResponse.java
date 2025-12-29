package com.achat.board.dto;

import com.achat.board.Board;
import com.achat.board.BoardCategory;
import java.time.LocalDateTime;

public record BoardResponse(
        Long id,
        String authorId,
        String authorNickname,
        BoardCategory category,
        String categoryName,
        String title,
        int viewCount,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt
) {
    public static BoardResponse from(Board board) {
        return new BoardResponse(
                board.getId(),
                board.getAuthorId(),
                board.getAuthorNickname(),
                board.getCategory(),
                board.getCategory().getDisplayName(),
                board.getTitle(),
                board.getViewCount(),
                board.getLikeCount(),
                board.getCommentCount(),
                board.getCreatedAt()
        );
    }
}