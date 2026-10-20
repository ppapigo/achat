package com.achat.quest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface QuestRepository extends JpaRepository<Quest, String> {
    Optional<Quest> findByMessageId(String messageId);
    List<Quest> findByRoomIdAndCompletedFalseAndCancelledFalse(String roomId);
}
