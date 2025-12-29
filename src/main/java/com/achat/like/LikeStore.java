package com.achat.like;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LikeStore {

    // postId -> Set<userId>
    private final Map<String, Set<String>> postLikes = new ConcurrentHashMap<>();

    public boolean add(String postId, String userId) {
        Set<String> likes = postLikes.computeIfAbsent(postId, k -> ConcurrentHashMap.newKeySet());
        return likes.add(userId);
    }

    public boolean remove(String postId, String userId) {
        Set<String> likes = postLikes.get(postId);
        if (likes == null) return false;
        return likes.remove(userId);
    }

    public boolean exists(String postId, String userId) {
        Set<String> likes = postLikes.get(postId);
        return likes != null && likes.contains(userId);
    }

    public int count(String postId) {
        Set<String> likes = postLikes.get(postId);
        return likes != null ? likes.size() : 0;
    }
}