package com.achat.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class LikeStore {
    private final ContentLikeRepository repository;

    public boolean add(String postId, String userId) {
        if (exists(postId, userId)) return false;
        repository.save(new ContentLike(ContentLike.TargetType.POST, postId, userId));
        return true;
    }
    public boolean remove(String postId, String userId) {
        return repository.findByTargetTypeAndTargetIdAndUserId(ContentLike.TargetType.POST, postId, userId).map(like -> {
            repository.delete(like);
            return true;
        }).orElse(false);
    }
    public boolean exists(String postId, String userId) {
        return repository.existsByTargetTypeAndTargetIdAndUserId(ContentLike.TargetType.POST, postId, userId);
    }
    public int count(String postId) { return Math.toIntExact(repository.countByTargetTypeAndTargetId(ContentLike.TargetType.POST, postId)); }
}
