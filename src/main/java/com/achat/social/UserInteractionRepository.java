package com.achat.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Transactional(readOnly = true)
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {
    Optional<UserInteraction> findByUserIdAndTargetUserId(String userId, String targetUserId);
    List<UserInteraction> findByUserIdAndLastInteractionAtAfter(String userId, Instant cutoff);
}
