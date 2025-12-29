package com.achat.augment;

import com.achat.quest.QuestType;

public class AugmentContext {
    private final String odLRXnwB;
    private final String roomId;
    private final String originalMessage;
    private QuestType questType;  // ✅ 추가

    public AugmentContext(String odLRXnwB, String roomId, String originalMessage) {
        this.odLRXnwB = odLRXnwB;
        this.roomId = roomId;
        this.originalMessage = originalMessage;
    }

    public String getUserId() { return odLRXnwB; }
    public String getRoomId() { return roomId; }
    public String getOriginalMessage() { return originalMessage; }

    // ✅ 추가
    public QuestType getQuestType() { return questType; }
    public void setQuestType(QuestType questType) { this.questType = questType; }
}