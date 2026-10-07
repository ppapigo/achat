package com.achat.minihome;

import com.achat.like.ContentLike;
import com.achat.like.ContentLikeRepository;
import com.achat.post.Post;
import com.achat.post.PostStore;
import com.achat.post.PostType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MiniHomeService {
    private final MiniHomePostRepository repository;
    private final ContentLikeRepository likeRepository;
    private final PostStore postStore;

    public MiniHomePost createPost(String userId, String content) {
        Post feedPost = new Post(userId, PostType.FREE, content);
        postStore.save(feedPost);
        return repository.save(new MiniHomePost(feedPost.getId(), userId, content));
    }

    @Transactional(readOnly = true)
    public List<MiniHomePost> getPostsByUser(String userId, int limit) {
        return limit <= 0 ? List.of() : repository.findByAuthorIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }

    public boolean toggleLike(String postId, String userId) {
        MiniHomePost post = repository.findById(postId).orElse(null);
        if (post == null) return false;
        var like = likeRepository.findByTargetTypeAndTargetIdAndUserId(ContentLike.TargetType.MINI_HOME, postId, userId);
        if (like.isPresent()) {
            likeRepository.delete(like.get());
            post.removeLike();
            return false;
        }
        likeRepository.save(new ContentLike(ContentLike.TargetType.MINI_HOME, postId, userId));
        post.addLike();
        return true;
    }

    @Transactional(readOnly = true)
    public boolean isLiked(String postId, String userId) {
        return likeRepository.existsByTargetTypeAndTargetIdAndUserId(ContentLike.TargetType.MINI_HOME, postId, userId);
    }

    public void deletePost(String postId, String userId) {
        repository.findById(postId).filter(p -> p.getAuthorId().equals(userId)).ifPresent(post -> {
            likeRepository.deleteByTargetTypeAndTargetId(ContentLike.TargetType.MINI_HOME, postId);
            repository.delete(post);
            postStore.delete(postId);
        });
    }
}
