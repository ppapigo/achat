package com.achat.message;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageCooldownStore {

    // roomId:userId -> UserMessageTracker
    private final Map<String, UserMessageTracker> trackers = new ConcurrentHashMap<>();

    private String key(String roomId, String userId) {
        return roomId + ":" + userId;  // 수정: odUserId → userId
    }

    public UserMessageTracker getTracker(String roomId, String userId) {
        return trackers.computeIfAbsent(key(roomId, userId), k -> new UserMessageTracker());
    }

    public void clearUser(String roomId, String userId) {
        trackers.remove(key(roomId, userId));
    }

    public void clearRoom(String roomId) {
        trackers.entrySet().removeIf(e -> e.getKey().startsWith(roomId + ":"));
    }
}