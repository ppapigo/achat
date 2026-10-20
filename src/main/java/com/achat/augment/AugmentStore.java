package com.achat.augment;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

@Component
public class AugmentStore {

    private final Map<String, UserAugmentState> states = new ConcurrentHashMap<>();

    private String key(String odUserId, String roomId) {
        return odUserId + ":" + roomId;
    }

    public UserAugmentState saveIfAbsent(UserAugmentState state) {
        UserAugmentState existing = states.putIfAbsent(key(state.getUserId(), state.getRoomId()), state);
        return existing == null ? state : existing;
    }

    public Optional<UserAugmentState> find(String odUserId, String roomId) {
        return Optional.ofNullable(states.get(key(odUserId, roomId)));
    }

    public void remove(String odUserId, String roomId) {
        states.remove(key(odUserId, roomId));
    }

    public List<UserAugmentState> inRoom(String roomId) {
        return states.values().stream().filter(state -> state.getRoomId().equals(roomId)).toList();
    }

    public Optional<UserAugmentState> received(String roomId, String roundId, String targetId) {
        return inRoom(roomId).stream().filter(state -> state.getRoundId().equals(roundId)
                && state.hasSelected() && targetId.equals(state.getTargetUserId())).findFirst();
    }

    /** Caller must hold RoomExecution for this room. Sender and recipient are reserved together. */
    public void select(UserAugmentState state, AugmentType type, String targetId) {
        if (received(state.getRoomId(), state.getRoundId(), targetId).isPresent()) {
            throw new AugmentConflictException("이미 증강을 받은 상대입니다. 다른 상대를 선택해주세요");
        }
        state.select(type, targetId);
    }

    public void clearRoom(String roomId) {
        com.achat.common.RoomExecution.checkpoint("augment-clear:" + roomId, () -> {
            var saved = inRoom(roomId);
            return () -> {
                states.entrySet().removeIf(entry -> entry.getValue().getRoomId().equals(roomId));
                saved.forEach(state -> states.put(key(state.getUserId(), roomId), state));
            };
        });
        states.entrySet().removeIf(entry -> entry.getValue().getRoomId().equals(roomId));
    }
}
