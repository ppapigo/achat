package com.achat.social;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 최근 상호작용 유저 저장소
 * - 같은 방에서 채팅한 유저 기록
 */
@Component
public class InteractionStore {

    // userId -> Map<targetUserId, lastInteractionTime>
    private final Map<String, Map<String, Instant>> interactions = new ConcurrentHashMap<>();

    /**
     * 상호작용 기록 (양방향)
     */
    public void record(String userId1, String userId2) {
        if (userId1.equals(userId2)) return;

        Instant now = Instant.now();

        interactions.computeIfAbsent(userId1, k -> new ConcurrentHashMap<>())
                .put(userId2, now);

        interactions.computeIfAbsent(userId2, k -> new ConcurrentHashMap<>())
                .put(userId1, now);
    }

    /**
     * 최근 N일 내 상호작용 유저 목록
     */
    public Set<String> getRecent(String userId, int days) {
        Map<String, Instant> userInteractions = interactions.get(userId);
        if (userInteractions == null) return Collections.emptySet();

        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

        return userInteractions.entrySet().stream()
                .filter(e -> e.getValue().isAfter(cutoff))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}