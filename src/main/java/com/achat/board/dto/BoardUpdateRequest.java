package com.achat.board.dto;

import com.achat.board.BoardCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BoardUpdateRequest(
        @NotNull BoardCategory category,
        @NotBlank @Size(min = 2, max = 100) String title,
        @NotBlank @Size(min = 10, max = 10000) String content
) {}