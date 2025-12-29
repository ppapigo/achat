package com.achat.quest;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QuestStore {

    // messageId → Quest
    private final Map<String, Quest> questsByMessage = new ConcurrentHashMap<>();

    public void save(Quest quest) {
        questsByMessage.put(quest.getMessageId(), quest);
    }

    public Optional<Quest> findByMessageId(String messageId) {
        Quest quest = questsByMessage.get(messageId);
        if (quest != null && quest.isExpired()) {
            questsByMessage.remove(messageId);
            return Optional.empty();
        }
        return Optional.ofNullable(quest);
    }

    public void remove(String messageId) {
        questsByMessage.remove(messageId);
    }

    // 만료된 퀘스트 정리
    public void cleanExpired() {
        questsByMessage.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}