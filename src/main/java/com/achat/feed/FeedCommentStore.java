package com.achat.feed;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class FeedCommentStore {

    // postId -> List<FeedComment>
    private final Map<String, List<FeedComment>> commentsByPost = new ConcurrentHashMap<>();

    public FeedComment save(FeedComment comment) {
        commentsByPost.computeIfAbsent(comment.getPostId(), k -> new ArrayList<>())
                .add(comment);
        return comment;
    }

    public List<FeedComment> findByPostId(String postId) {
        return commentsByPost.getOrDefault(postId, new ArrayList<>())
                .stream()
                .sorted(Comparator.comparing(FeedComment::getCreatedAt))
                .collect(Collectors.toList());
    }

    public int countByPostId(String postId) {
        return commentsByPost.getOrDefault(postId, List.of()).size();
    }

    public void delete(String commentId, String postId) {
        List<FeedComment> comments = commentsByPost.get(postId);
        if (comments != null) {
            comments.removeIf(c -> c.getId().equals(commentId));
        }
    }
}