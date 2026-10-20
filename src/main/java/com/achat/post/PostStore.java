package com.achat.post;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import java.time.*;
import com.achat.feed.FeedCommentRepository;
import com.achat.like.ContentLike;
import com.achat.like.ContentLikeRepository;
@Component
@Transactional
@RequiredArgsConstructor
public class PostStore {
    private final PostRepository repository;
    private final PostQuotaRepository quotaRepository;
    private final FeedCommentRepository commentRepository;
    private final ContentLikeRepository likeRepository;

    public void save(Post post) {
        boolean newPost = !repository.existsById(post.getId());
        repository.save(post);
        if (newPost) {
            PostQuota quota = quotaRepository.findById(post.getAuthorId()).orElseGet(() -> new PostQuota(post.getAuthorId()));
            quota.recordPost();
            quotaRepository.save(quota);
        }
    }
    public Optional<Post> findById(String id) { return repository.findById(id); }
    public List<Post> findByAuthorId(String authorId, int limit) {
        return limit <= 0 ? List.of() : repository.findByAuthorIdOrderByCreatedAtDesc(authorId, PageRequest.of(0, Math.min(limit, 100)));
    }
    public List<Post> findByAuthorIds(Set<String> authorIds, int limit) {
        return limit <= 0 || authorIds.isEmpty() ? List.of() : repository.findByAuthorIdInOrderByCreatedAtDesc(authorIds, PageRequest.of(0, Math.min(limit, 101)));
    }
    public void delete(String postId) {
        commentRepository.deleteByPostId(postId);
        likeRepository.deleteByTargetTypeAndTargetId(ContentLike.TargetType.POST, postId);
        repository.deleteById(postId);
    }
    public Optional<Instant> getLastPostTime(String authorId) {
        return quotaRepository.findById(authorId).map(PostQuota::getLastPostAt);
    }
    public int getDailyCount(String authorId) { return quotaRepository.findById(authorId).map(PostQuota::getDailyCount).orElse(0); }
}
