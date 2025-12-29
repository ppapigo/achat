package com.achat.board;

import com.achat.board.dto.*;
import com.achat.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    // 게시글 목록 (검색 포함)
    @GetMapping
    public ApiResponse<Map<String, Object>> getBoards(
            @RequestParam(required = false) BoardCategory category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<BoardResponse> boards = boardService.getBoards(category, keyword, page, size);
        int total = boardService.getTotalCount(category, keyword);

        return ApiResponse.ok(Map.of(
                "boards", boards,
                "total", total,
                "page", page,
                "size", size,
                "keyword", keyword != null ? keyword : "",
                "hasNext", (page + 1) * size < total
        ));
    }

    // 게시글 상세
    @GetMapping("/{boardId}")
    public ApiResponse<BoardDetailResponse> getBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal String userId) {
        return ApiResponse.ok(boardService.getBoard(boardId, userId));
    }

    // 게시글 작성
    @PostMapping
    public ApiResponse<BoardResponse> createBoard(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody BoardCreateRequest request) {
        return ApiResponse.ok(boardService.createBoard(userId, request));
    }

    // 게시글 수정
    @PutMapping("/{boardId}")
    public ApiResponse<BoardResponse> updateBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody BoardUpdateRequest request) {
        return ApiResponse.ok(boardService.updateBoard(boardId, userId, request));
    }

    // 게시글 삭제
    @DeleteMapping("/{boardId}")
    public ApiResponse<Map<String, Object>> deleteBoard(
            @PathVariable Long boardId,
            @AuthenticationPrincipal String userId) {
        boardService.deleteBoard(boardId, userId);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    // 게시글 좋아요
    @PostMapping("/{boardId}/like")
    public ApiResponse<Map<String, Object>> toggleLike(
            @PathVariable Long boardId,
            @AuthenticationPrincipal String userId) {
        int likeCount = boardService.toggleBoardLike(boardId, userId);
        return ApiResponse.ok(Map.of("likeCount", likeCount));
    }

    // 댓글 작성
    @PostMapping("/{boardId}/comments")
    public ApiResponse<CommentResponse> createComment(
            @PathVariable Long boardId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.ok(boardService.createComment(boardId, userId, request));
    }

    // 댓글 수정
    @PutMapping("/{boardId}/comments/{commentId}")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CommentUpdateRequest request) {
        return ApiResponse.ok(boardService.updateComment(boardId, commentId, userId, request));
    }

    // 댓글 삭제
    @DeleteMapping("/{boardId}/comments/{commentId}")
    public ApiResponse<Map<String, Object>> deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal String userId) {
        boardService.deleteComment(boardId, commentId, userId);
        return ApiResponse.ok(Map.of("deleted", true));
    }

    // 카테고리 목록
    @GetMapping("/categories")
    public ApiResponse<List<Map<String, String>>> getCategories() {
        List<Map<String, String>> categories = java.util.Arrays.stream(BoardCategory.values())
                .map(c -> Map.of("value", c.name(), "label", c.getDisplayName()))
                .toList();
        return ApiResponse.ok(categories);
    }
}