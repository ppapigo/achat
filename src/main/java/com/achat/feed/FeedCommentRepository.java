package com.achat.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface FeedCommentRepository extends JpaRepository<FeedComment, String> {
    List<FeedComment> findByPostIdOrderByCreatedAtAsc(String postId);
    long countByPostId(String postId);
    long deleteByIdAndPostId(String id, String postId);
    void deleteByPostId(String postId);
}
