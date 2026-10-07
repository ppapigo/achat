package com.achat.room;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class RoomStore {
    private final RoomRepository repository;

    public void save(Room room) { repository.save(room); }
    public Optional<Room> findById(String id) { return repository.findById(id); }
    public List<Room> findAll() { return repository.findAll(); }
    public void remove(String roomId) { repository.deleteById(roomId); }
    public void delete(String roomId) { remove(roomId); }
}
