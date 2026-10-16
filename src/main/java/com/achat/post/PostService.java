package com.achat.post;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.like.LikeStore;
import com.achat.post.dto.PostDto.CreatePostRequest;
import com.achat.post.dto.PostDto.PostResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    private static final Duration POST_COOLDOWN = Duration.ofMinutes(3);
    private static final int DAILY_LIMIT = 10;

    private final PostStore postStore;
    private final LikeStore likeStore;

    public PostResponse createPost(String authorId, CreatePostRequest request) {
        // 쿨타임 체크
        postStore.getLastPostTime(authorId).ifPresent(lastTime -> {
            Duration elapsed = Duration.between(lastTime, Instant.now());
            if (elapsed.compareTo(POST_COOLDOWN) < 0) {
                long remaining = POST_COOLDOWN.minus(elapsed).toSeconds();
                throw new IllegalStateException(remaining + "초 후에 작성 가능합니다");
            }
        });

        // 일일 제한 체크
        int todayCount = postStore.getDailyCount(authorId);
        if (todayCount >= DAILY_LIMIT) {
            throw new IllegalStateException("오늘 작성 한도(" + DAILY_LIMIT + "개)를 초과했습니다");
        }

        // 생성
        Post post = new Post(authorId, request.type(), request.content());
        if (request.tags() != null) {
            post.setTags(request.tags());
        }

        postStore.save(post);

        return PostResponse.from(post, false);
    }

    public List<PostResponse> getPostsByAuthor(String authorId, String viewerId, int limit) {
        List<Post> posts = postStore.findByAuthorId(authorId, limit);

        return posts.stream()
                .map(post -> PostResponse.from(post, likeStore.exists(post.getId(), viewerId)))
                .collect(Collectors.toList());
    }

    public List<PostResponse> getPostsByAuthors(Set<String> authorIds, String viewerId, int limit) {
        List<Post> posts = postStore.findByAuthorIds(authorIds, limit);

        return posts.stream()
                .map(post -> PostResponse.from(post, likeStore.exists(post.getId(), viewerId)))
                .collect(Collectors.toList());
    }

    public PostResponse getPost(String postId, String viewerId) {
        Post post = postStore.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("포스트를 찾을 수 없습니다"));
        return PostResponse.from(post, likeStore.exists(postId, viewerId));
    }
}
