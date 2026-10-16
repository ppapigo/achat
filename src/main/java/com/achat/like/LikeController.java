package com.achat.like;

import lombok.RequiredArgsConstructor;

import com.achat.common.ApiResponse;
import com.achat.like.LikeService.LikeResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /**
     * 좋아요 토글
     */
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<LikeResult>> toggleLike(
            @PathVariable String postId,
            @RequestAttribute("userId") String userId
    ) {
        LikeResult result = likeService.toggle(postId, userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
