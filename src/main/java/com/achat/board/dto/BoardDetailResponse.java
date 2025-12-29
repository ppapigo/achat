package com.achat.board.dto;

import com.achat.board.Board;
import com.achat.board.BoardCategory;
import java.time.LocalDateTime;
import java.util.List;

public record BoardDetailResponse(
        Long id,
        String authorId,
        String authorNickname,
        BoardCategory category,
        String categoryName,
        String title,
        String content,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean liked,
        LocalDateTime createdAt,
        List<CommentResponse> comments
) {
    public static BoardDetailResponse from(Board board, List<CommentResponse> comments, boolean liked) {
        return new BoardDetailResponse(
                board.getId(),
                board.getAuthorId(),
                board.getAuthorNickname(),
                board.getCategory(),
                board.getCategory().getDisplayName(),
                board.getTitle(),
                board.getContent(),
                board.getViewCount(),
                board.getLikeCount(),
                board.getCommentCount(),
                liked,
                board.getCreatedAt(),
                comments
        );
    }
}