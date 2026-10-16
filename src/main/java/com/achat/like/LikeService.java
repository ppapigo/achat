package com.achat.like;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.post.Post;
import com.achat.post.PostStore;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class LikeService {

    private final LikeStore likeStore;
    private final PostStore postStore;

    /**
     * 좋아요 토글
     * - 안 눌렀으면 추가
     * - 눌렀으면 취소
     */
    public LikeResult toggle(String postId, String userId) {
        Post post = postStore.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("포스트를 찾을 수 없습니다"));

        boolean liked;
        if (likeStore.exists(postId, userId)) {
            likeStore.remove(postId, userId);
            post.decrementLike();
            liked = false;
        } else {
            likeStore.add(postId, userId);
            post.incrementLike();
            liked = true;
        }

        return new LikeResult(liked, post.getLikeCount());
    }

    /**
     * 좋아요 여부 확인
     */
    public boolean isLiked(String postId, String userId) {
        return likeStore.exists(postId, userId);
    }

    public record LikeResult(
            boolean liked,
            long likeCount
    ) {}
}
