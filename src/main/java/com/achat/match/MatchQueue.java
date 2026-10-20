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

    public synchronized void join(String userId, int preferredCount) {
        if (joinTimes.containsKey(userId)) return;
        waitingUsers.add(userId);
        joinTimes.put(userId, System.currentTimeMillis());
        userPreference.put(userId, preferredCount);
    }

    public synchronized void leave(String userId) {
        waitingUsers.remove(userId);
        joinTimes.remove(userId);
        userPreference.remove(userId);
    }

    public synchronized boolean isWaiting(String userId) {
        return waitingUsers.contains(userId);
    }

    public synchronized int getQueueSize() {
        return waitingUsers.size();
    }

    public synchronized long getWaitTime(String userId) {
        Long joinTime = joinTimes.get(userId);
        if (joinTime == null) return 0;
        return System.currentTimeMillis() - joinTime;
    }

    public synchronized int getMostPreferredCount() {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Integer pref : userPreference.values()) {
            counts.merge(pref, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(6);  // 테스트용 기본값
    }

    public synchronized int getOptimalPlayerCount() {
        int queueSize = waitingUsers.size();
        int preferred = getMostPreferredCount();

        if (queueSize >= 12) return 12;
        if (queueSize >= 10) return Math.min(preferred, 10);
        if (queueSize >= 8) return Math.min(preferred, 8);
        if (queueSize >= 6) return 6;

        return 6;  // 테스트용 기본값
    }

    public synchronized long getMaxWaitTime() {
        return joinTimes.values().stream()
                .mapToLong(t -> System.currentTimeMillis() - t)
                .max()
                .orElse(0);
    }

    public synchronized List<String> pollPlayers(int count) {
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
    public synchronized Runnable checkpoint() {
        var users = new ArrayList<>(waitingUsers);
        var times = new HashMap<>(joinTimes);
        var preferences = new HashMap<>(userPreference);
        return () -> {
            synchronized (this) {
                waitingUsers.clear(); waitingUsers.addAll(users);
                joinTimes.clear(); joinTimes.putAll(times);
                userPreference.clear(); userPreference.putAll(preferences);
            }
        };
    }
}
