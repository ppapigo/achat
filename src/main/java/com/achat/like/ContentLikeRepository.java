package com.achat.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Transactional(readOnly = true)
public interface ContentLikeRepository extends JpaRepository<ContentLike, Long> {
    Optional<ContentLike> findByTargetTypeAndTargetIdAndUserId(ContentLike.TargetType type, String targetId, String userId);
    boolean existsByTargetTypeAndTargetIdAndUserId(ContentLike.TargetType type, String targetId, String userId);
    long countByTargetTypeAndTargetId(ContentLike.TargetType type, String targetId);
    @Transactional
    void deleteByTargetTypeAndTargetId(ContentLike.TargetType type, String targetId);
}
