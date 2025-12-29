package com.achat.minihome;

import com.achat.post.Post;
import com.achat.post.PostStore;
import com.achat.post.PostType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MiniHomeService {

    private final Map<String, MiniHomePost> posts = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> likes = new ConcurrentHashMap<>();

    private final PostStore postStore;

    public MiniHomeService(PostStore postStore) {
        this.postStore = postStore;
    }

    public MiniHomePost createPost(String userId, String content) {
        String id = UUID.randomUUID().toString();
        MiniHomePost miniPost = new MiniHomePost(id, userId, content);
        posts.put(id, miniPost);

        // ⭐ 피드에도 추가!
        Post feedPost = new Post(userId, PostType.FREE, content);
        postStore.save(feedPost);

        return miniPost;
    }

    public List<MiniHomePost> getPostsByUser(String userId, int limit) {
        return posts.values().stream()
                .filter(p -> p.getAuthorId().equals(userId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .toList();
    }

    public boolean toggleLike(String postId, String userId) {
        MiniHomePost post = posts.get(postId);
        if (post == null) return false;

        Set<String> postLikes = likes.computeIfAbsent(postId, k -> new HashSet<>());

        if (postLikes.contains(userId)) {
            postLikes.remove(userId);
            post.removeLike();
            return false;
        } else {
            postLikes.add(userId);
            post.addLike();
            return true;
        }
    }

    public boolean isLiked(String postId, String userId) {
        return likes.getOrDefault(postId, Set.of()).contains(userId);
    }

    public void deletePost(String postId, String userId) {
        MiniHomePost post = posts.get(postId);
        if (post != null && post.getAuthorId().equals(userId)) {
            posts.remove(postId);
            likes.remove(postId);
            postStore.delete(postId);
        }
    }
}