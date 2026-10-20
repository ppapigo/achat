package com.achat.quest;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional
public class QuestStore {

    // messageId → Quest
    private final QuestRepository repository;

    public void save(Quest quest) {
        repository.save(quest);
    }

    public Optional<Quest> findByMessageId(String messageId) {
        return repository.findByMessageId(messageId);
    }

    public void remove(String messageId) {
        repository.findByMessageId(messageId).ifPresent(Quest::cancel);
    }

    // 만료된 퀘스트 정리
    public void cleanExpired() {
        // Expiration is checked on reads/progression; keep completed receipts for idempotency.
    }
    public void cancelRoom(String roomId) {
        repository.findByRoomIdAndCompletedFalseAndCancelledFalse(roomId).forEach(Quest::cancel);
    }
}
