package com.achat.message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Transactional(readOnly = true)
public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByRoomIdOrderByCreatedAtAsc(String roomId);
    List<Message> findByRoomIdOrderByCreatedAtDescIdDesc(String roomId, Pageable pageable);
}
