package com.achat.round;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoundStore {

    private final Map<String, Round> rounds = new ConcurrentHashMap<>();

    public void save(Round round) {
        rounds.put(round.getRoomId(), round);
    }

    public Optional<Round> findByRoomId(String roomId) {
        return Optional.ofNullable(rounds.get(roomId));
    }

    public void remove(String roomId) {
        rounds.remove(roomId);
    }

    public void delete(String roomId) {
        rounds.remove(roomId);
    }

    public void checkpoint(String roomId) {
        com.achat.common.RoomExecution.checkpoint("round:" + roomId, () -> {
            Round saved = rounds.get(roomId);
            Runnable restore = saved == null ? () -> {} : saved.checkpoint();
            return () -> {
                restore.run();
                if (saved == null) rounds.remove(roomId);
                else rounds.put(roomId, saved);
            };
        });
    }
}
