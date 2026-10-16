package com.achat.post;

import lombok.RequiredArgsConstructor;

import com.achat.common.ApiResponse;
import com.achat.post.dto.PostDto.CreatePostRequest;
import com.achat.post.dto.PostDto.PostListResponse;
import com.achat.post.dto.PostDto.PostResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 포스트 작성
     */
    @PostMapping("/me/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody CreatePostRequest request
    ) {
        PostResponse post = postService.createPost(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(post));
    }

    /**
     * 내 포스트 목록
     */
    @GetMapping("/me/posts")
    public ResponseEntity<ApiResponse<PostListResponse>> getMyPosts(
            @RequestAttribute("userId") String userId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<PostResponse> posts = postService.getPostsByAuthor(userId, userId, limit);
        boolean hasMore = posts.size() >= limit;
        return ResponseEntity.ok(ApiResponse.ok(new PostListResponse(posts, hasMore)));
    }

    /**
     * 특정 유저 포스트 목록
     */
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<ApiResponse<PostListResponse>> getUserPosts(
            @PathVariable String userId,
            @RequestAttribute("userId") String viewerId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        List<PostResponse> posts = postService.getPostsByAuthor(userId, viewerId, limit);
        boolean hasMore = posts.size() >= limit;
        return ResponseEntity.ok(ApiResponse.ok(new PostListResponse(posts, hasMore)));
    }

    /**
     * 단일 포스트 조회
     */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable String postId,
            @RequestAttribute("userId") String viewerId
    ) {
        PostResponse post = postService.getPost(postId, viewerId);
        return ResponseEntity.ok(ApiResponse.ok(post));
    }
}
