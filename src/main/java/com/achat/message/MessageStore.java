package com.achat.message;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageStore {

    private final Map<String, List<Message>> messagesByRoom = new ConcurrentHashMap<>();
    private final Map<String, Message> messagesById = new ConcurrentHashMap<>();  // 추가!

    public void save(Message message) {
        messagesByRoom
                .computeIfAbsent(message.getRoomId(),
                        k -> Collections.synchronizedList(new ArrayList<>()))
                .add(message);

        messagesById.put(message.getId(), message);  // 추가!
    }

    public List<Message> findByRoomId(String roomId) {
        List<Message> messages = messagesByRoom.get(roomId);
        if (messages == null) {
            return List.of();
        }
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    // 추가!
    public Optional<Message> findById(String messageId) {
        return Optional.ofNullable(messagesById.get(messageId));
    }
}