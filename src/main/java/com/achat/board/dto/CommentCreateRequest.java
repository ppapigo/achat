package com.achat.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        Long parentId,  // null이면 일반 댓글, 값 있으면 대댓글
        @NotBlank @Size(min = 1, max = 1000) String content
) {}