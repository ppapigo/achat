package com.achat.feed;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.friend.FriendStore;
import com.achat.like.LikeStore;
import com.achat.post.Post;
import com.achat.post.PostStore;
import com.achat.post.dto.PostDto.PostResponse;
import com.achat.social.InteractionStore;
import com.achat.user.UserStore;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class FeedService {

    private static final int INTERACTION_DAYS = 7;
    private static final int DEFAULT_LIMIT = 20;

    private final PostStore postStore;
    private final LikeStore likeStore;
    private final InteractionStore interactionStore;
    private final FriendStore friendStore;
    private final UserStore userStore;
    private final FeedCommentStore feedCommentStore;  // 추가!

    public FeedResponse getFeed(String userId, int limit) {
        Set<String> feedUserIds = new HashSet<>();

        feedUserIds.addAll(friendStore.getFriendIds(userId));
        feedUserIds.addAll(interactionStore.getRecent(userId, INTERACTION_DAYS));
        feedUserIds.add(userId);

        if (feedUserIds.isEmpty()) {
            return new FeedResponse(List.of(), false);
        }

        int fetchLimit = limit > 0 ? Math.min(limit, 100) : DEFAULT_LIMIT;
        List<Post> posts = postStore.findByAuthorIds(feedUserIds, fetchLimit + 1);

        boolean hasMore = posts.size() > fetchLimit;
        if (hasMore) {
            posts = posts.subList(0, fetchLimit);
        }

        List<PostResponse> responses = posts.stream()
                .map(post -> {
                    String authorUsername = userStore.findById(post.getAuthorId())
                            .map(user -> user.getUsername())
                            .orElse(null);
                    int commentCount = feedCommentStore.countByPostId(post.getId());  // 추가!
                    return PostResponse.from(post, likeStore.exists(post.getId(), userId), authorUsername, commentCount);
                })
                .collect(Collectors.toList());

        return new FeedResponse(responses, hasMore);
    }

    // ===== 댓글 메서드 추가 =====

    public List<CommentResponse> getComments(String postId) {
        return feedCommentStore.findByPostId(postId).stream()
                .map(comment -> {
                    String username = userStore.findById(comment.getAuthorId())
                            .map(u -> u.getUsername())
                            .orElse(null);
                    return new CommentResponse(
                            comment.getId(),
                            comment.getAuthorId(),
                            username,
                            comment.getContent(),
                            comment.getCreatedAt().toString()
                    );
                })
                .collect(Collectors.toList());
    }

    public CommentResponse addComment(String postId, String userId, String content) {
        if (content == null || content.isBlank() || content.length() > 2000) throw new IllegalArgumentException("댓글은 1~2000자여야 합니다");
        if (postStore.findById(postId).isEmpty()) throw new IllegalArgumentException("게시글을 찾을 수 없습니다");
        FeedComment comment = new FeedComment(postId, userId, content);
        feedCommentStore.save(comment);

        String username = userStore.findById(userId)
                .map(u -> u.getUsername())
                .orElse(null);

        return new CommentResponse(
                comment.getId(),
                comment.getAuthorId(),
                username,
                comment.getContent(),
                comment.getCreatedAt().toString()
        );
    }

    public void deleteComment(String postId, String commentId) {
        feedCommentStore.delete(commentId, postId);
    }

    // ===== Records =====

    public record FeedResponse(
            List<PostResponse> posts,
            boolean hasMore
    ) {}

    public record CommentResponse(
            String id,
            String authorId,
            String authorUsername,
            String content,
            String createdAt
    ) {}
}
