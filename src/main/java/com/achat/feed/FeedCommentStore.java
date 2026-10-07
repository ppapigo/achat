package com.achat.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class FeedCommentStore {
    private final FeedCommentRepository repository;

    public FeedComment save(FeedComment comment) { return repository.save(comment); }
    public List<FeedComment> findByPostId(String postId) { return repository.findByPostIdOrderByCreatedAtAsc(postId); }
    public int countByPostId(String postId) { return Math.toIntExact(repository.countByPostId(postId)); }
    public void delete(String commentId, String postId) { repository.deleteByIdAndPostId(commentId, postId); }
}
