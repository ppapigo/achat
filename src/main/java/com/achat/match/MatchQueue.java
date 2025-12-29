package com.achat.match;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class MatchQueue {

    private final Queue<String> waitingUsers = new ConcurrentLinkedQueue<>();
    private final Map<String, Long> joinTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> userPreference = new ConcurrentHashMap<>();

    public void join(String userId, int preferredCount) {
        leave(userId);
        waitingUsers.add(userId);
        joinTimes.put(userId, System.currentTimeMillis());
        userPreference.put(userId, preferredCount);
    }

    public void leave(String userId) {
        waitingUsers.remove(userId);
        joinTimes.remove(userId);
        userPreference.remove(userId);
    }

    public boolean isWaiting(String userId) {
        return waitingUsers.contains(userId);
    }

    public int getQueueSize() {
        return waitingUsers.size();
    }

    public long getWaitTime(String userId) {
        Long joinTime = joinTimes.get(userId);
        if (joinTime == null) return 0;
        return System.currentTimeMillis() - joinTime;
    }

    public int getMostPreferredCount() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Integer pref : userPreference.values()) {
            counts.merge(pref, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(6);  // 테스트용 기본값
    }

    public int getOptimalPlayerCount() {
        int queueSize = waitingUsers.size();
        int preferred = getMostPreferredCount();

        if (queueSize >= 12) return 12;
        if (queueSize >= 10) return Math.min(preferred, 10);
        if (queueSize >= 8) return Math.min(preferred, 8);
        if (queueSize >= 6) return 6;

        return 6;  // 테스트용 기본값
    }

    public long getMaxWaitTime() {
        return joinTimes.values().stream()
                .mapToLong(t -> System.currentTimeMillis() - t)
                .max()
                .orElse(0);
    }

    public List<String> pollPlayers(int count) {
        List<String> players = new ArrayList<>();
        for (int i = 0; i < count && !waitingUsers.isEmpty(); i++) {
            String userId = waitingUsers.poll();
            if (userId != null) {
                joinTimes.remove(userId);
                userPreference.remove(userId);
                players.add(userId);
            }
        }
        return players;
    }
}