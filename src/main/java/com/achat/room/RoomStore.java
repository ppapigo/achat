package com.achat.room;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomStore {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public void save(Room room) {
        rooms.put(room.getId(), room);
    }

    public Optional<Room> findById(String id) {
        return Optional.ofNullable(rooms.get(id));
    }

    public List<Room> findAll() {
        return new ArrayList<>(rooms.values());
    }

    public void remove(String roomId) {
        rooms.remove(roomId);
    }

    // RoomStore.java에 추가
    public void delete(String roomId) {
        rooms.remove(roomId);
    }


}