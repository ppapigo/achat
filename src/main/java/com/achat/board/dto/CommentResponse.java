package com.achat.board.dto;

import com.achat.board.BoardComment;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record CommentResponse(
        Long id,
        Long parentId,
        String authorId,
        String authorNickname,
        String content,
        LocalDateTime createdAt,
        List<CommentResponse> replies
) {
    public static CommentResponse from(BoardComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getParentId(),
                comment.getAuthorId(),
                comment.getAuthorNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                new ArrayList<>()  // replies는 Service에서 채움
        );
    }

    public static CommentResponse from(BoardComment comment, List<CommentResponse> replies) {
        return new CommentResponse(
                comment.getId(),
                comment.getParentId(),
                comment.getAuthorId(),
                comment.getAuthorNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                replies
        );
    }
}