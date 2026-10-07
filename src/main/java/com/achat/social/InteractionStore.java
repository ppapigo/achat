package com.achat.social;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class InteractionStore {
    private final UserInteractionRepository repository;

    public void record(String userId1, String userId2) {
        if (userId1.equals(userId2)) return;
        Instant now = Instant.now();
        recordOne(userId1, userId2, now);
        recordOne(userId2, userId1, now);
    }
    private void recordOne(String userId, String targetUserId, Instant now) {
        UserInteraction interaction = repository.findByUserIdAndTargetUserId(userId, targetUserId)
                .orElseGet(() -> new UserInteraction(userId, targetUserId));
        interaction.recordAt(now);
        repository.save(interaction);
    }
    public Set<String> getRecent(String userId, int days) {
        return repository.findByUserIdAndLastInteractionAtAfter(userId, Instant.now().minus(days, java.time.temporal.ChronoUnit.DAYS))
                .stream().map(UserInteraction::getTargetUserId).collect(java.util.stream.Collectors.toSet());
    }
}
