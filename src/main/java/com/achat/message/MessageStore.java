package com.achat.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class MessageStore {
    private final MessageRepository repository;

    public void save(Message message) { repository.save(message); }
    public List<Message> findByRoomId(String roomId) { return repository.findByRoomIdOrderByCreatedAtAsc(roomId); }
    public Optional<Message> findById(String id) { return repository.findById(id); }
    public List<Message> findRecentByRoomId(String roomId, int limit) {
        List<Message> recent = new ArrayList<>(repository.findByRoomIdOrderByCreatedAtDescIdDesc(
                roomId, PageRequest.of(0, Math.max(1, Math.min(100, limit)))));
        Collections.reverse(recent);
        return recent;
    }
}
