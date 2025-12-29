package com.achat.augment;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AugmentStore {

    private final Map<String, UserAugmentState> states = new ConcurrentHashMap<>();

    private String key(String odUserId, String roomId) {
        return odUserId + ":" + roomId;
    }

    public void save(UserAugmentState state) {
        states.put(key(state.getUserId(), state.getRoomId()), state);
    }

    public Optional<UserAugmentState> find(String odUserId, String roomId) {
        return Optional.ofNullable(states.get(key(odUserId, roomId)));
    }

    public void remove(String odUserId, String roomId) {
        states.remove(key(odUserId, roomId));
    }
}