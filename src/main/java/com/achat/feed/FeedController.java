package com.achat.feed;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping
    public ResponseEntity<?> getFeed(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        FeedService.FeedResponse response = feedService.getFeed(userId, limit);
        return ResponseEntity.ok(Map.of("data", response));
    }

    // ===== 댓글 API 추가 =====

    @GetMapping("/{postId}/comments")
    public ResponseEntity<?> getComments(@PathVariable String postId) {
        List<FeedService.CommentResponse> comments = feedService.getComments(postId);
        return ResponseEntity.ok(Map.of("data", comments));
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable String postId,
            @RequestAttribute("userId") String userId,
            @RequestBody AddCommentRequest request
    ) {
        FeedService.CommentResponse comment = feedService.addComment(postId, userId, request.content());
        return ResponseEntity.ok(Map.of("data", comment));
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId
    ) {
        feedService.deleteComment(postId, commentId);
        return ResponseEntity.ok(Map.of("message", "삭제됨"));
    }

    public record AddCommentRequest(String content) {}
}
