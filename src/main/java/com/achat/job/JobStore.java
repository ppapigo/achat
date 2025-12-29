package com.achat.job;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobStore {

    private final Map<String, UserJobState> states = new ConcurrentHashMap<>();

    public UserJobState getOrCreate(String odUserId) {
        return states.computeIfAbsent(odUserId, UserJobState::new);
    }

    public Optional<UserJobState> find(String odUserId) {
        return Optional.ofNullable(states.get(odUserId));
    }

    public void save(UserJobState state) {
        states.put(state.getUserId(), state);
    }
}