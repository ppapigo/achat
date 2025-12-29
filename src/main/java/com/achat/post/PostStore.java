package com.achat.post;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PostStore {

    private final Map<String, Post> posts = new ConcurrentHashMap<>();
    private final Map<String, List<String>> authorPosts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastPostTime = new ConcurrentHashMap<>();
    private final Map<String, Integer> dailyCount = new ConcurrentHashMap<>();
    private LocalDate lastResetDate = LocalDate.now();

    public void save(Post post) {
        posts.put(post.getId(), post);
        authorPosts.computeIfAbsent(post.getAuthorId(), k -> new ArrayList<>())
                .add(0, post.getId());
        lastPostTime.put(post.getAuthorId(), Instant.now());
        dailyCount.merge(post.getAuthorId(), 1, Integer::sum);
    }

    public Optional<Post> findById(String postId) {
        return Optional.ofNullable(posts.get(postId));
    }

    public List<Post> findByAuthorId(String authorId, int limit) {
        resetDailyCountIfNeeded();
        List<String> postIds = authorPosts.getOrDefault(authorId, Collections.emptyList());
        return postIds.stream()
                .limit(limit)
                .map(posts::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Post> findByAuthorIds(Set<String> authorIds, int limit) {
        return authorIds.stream()
                .flatMap(id -> authorPosts.getOrDefault(id, Collections.emptyList()).stream())
                .map(posts::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ⭐ 추가!
    public void delete(String postId) {
        Post post = posts.remove(postId);
        if (post != null) {
            List<String> authorPostIds = authorPosts.get(post.getAuthorId());
            if (authorPostIds != null) {
                authorPostIds.remove(postId);
            }
        }
    }

    public Optional<Instant> getLastPostTime(String authorId) {
        return Optional.ofNullable(lastPostTime.get(authorId));
    }

    public int getDailyCount(String authorId) {
        resetDailyCountIfNeeded();
        return dailyCount.getOrDefault(authorId, 0);
    }

    private void resetDailyCountIfNeeded() {
        LocalDate today = LocalDate.now();
        if (!today.equals(lastResetDate)) {
            dailyCount.clear();
            lastResetDate = today;
        }
    }
}