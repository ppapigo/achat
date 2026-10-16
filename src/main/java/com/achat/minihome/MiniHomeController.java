package com.achat.minihome;

import lombok.RequiredArgsConstructor;

import com.achat.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/minihome")
@RequiredArgsConstructor
public class MiniHomeController {

    private final MiniHomeService miniHomeService;

    // 내 글 작성
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPost(
            @RequestAttribute("userId") String userId,
            @RequestBody Map<String, String> body
    ) {
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("내용을 입력하세요"));
        }

        MiniHomePost post = miniHomeService.createPost(userId, content.trim());

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "id", post.getId(),
                "authorId", post.getAuthorId(),
                "content", post.getContent(),
                "likeCount", post.getLikeCount(),
                "liked", false,
                "createdAt", post.getCreatedAt().toString()
        )));
    }

    // 내 글 목록
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyPosts(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<MiniHomePost> posts = miniHomeService.getPostsByUser(userId, limit);

        List<Map<String, Object>> result = posts.stream().map(post -> Map.<String, Object>of(
                "id", post.getId(),
                "authorId", post.getAuthorId(),
                "content", post.getContent(),
                "likeCount", post.getLikeCount(),
                "liked", miniHomeService.isLiked(post.getId(), userId),
                "createdAt", post.getCreatedAt().toString()
        )).toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // 특정 유저 글 목록
    @GetMapping("/users/{targetUserId}/posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserPosts(
            @PathVariable String targetUserId,
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<MiniHomePost> posts = miniHomeService.getPostsByUser(targetUserId, limit);

        List<Map<String, Object>> result = posts.stream().map(post -> Map.<String, Object>of(
                "id", post.getId(),
                "authorId", post.getAuthorId(),
                "content", post.getContent(),
                "likeCount", post.getLikeCount(),
                "liked", miniHomeService.isLiked(post.getId(), userId),
                "createdAt", post.getCreatedAt().toString()
        )).toList();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // 좋아요 토글
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleLike(
            @PathVariable String postId,
            @RequestAttribute("userId") String userId
    ) {
        boolean liked = miniHomeService.toggleLike(postId, userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("liked", liked)));
    }

    // 글 삭제
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable String postId,
            @RequestAttribute("userId") String userId
    ) {
        miniHomeService.deletePost(postId, userId);
        return ResponseEntity.ok(ApiResponse.ok("삭제 완료"));
    }
}
