package com.achat.quest;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Quest {
    private final String id;
    private final String roomId;
    private final String messageId;      // 퀘스트가 붙은 메시지
    private final String creatorId;      // 퀘스트 생성자
    private final QuestType type;
    private final int targetCount;       // 목표 수 (예: 답장 3개)
    private final int rewardCoins;
    private final Instant createdAt;
    private final Instant expiresAt;

    private final Set<String> completedBy = new HashSet<>();  // 완료한 유저들
    private int currentCount = 0;
    private boolean completed = false;

    public Quest(String roomId, String messageId, String creatorId, QuestType type) {
        this.id = UUID.randomUUID().toString();
        this.roomId = roomId;
        this.messageId = messageId;
        this.creatorId = creatorId;
        this.type = type;
        this.targetCount = type.getDefaultTarget();
        this.rewardCoins = type.getRewardCoins();
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(300);  // 5분 제한
    }

    public boolean tryProgress(String odLRXnwB) {
        if (completed || isExpired()) return false;
        if (odLRXnwB.equals(creatorId)) return false;  // 본인은 제외
        if (completedBy.contains(odLRXnwB)) return false;  // 중복 방지

        currentCount++;
        completedBy.add(odLRXnwB);

        if (currentCount >= targetCount) {
            completed = true;
            return true;  // 퀘스트 완료!
        }
        return false;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // Getters
    public String getId() { return id; }
    public String getRoomId() { return roomId; }
    public String getMessageId() { return messageId; }
    public String getCreatorId() { return creatorId; }
    public QuestType getType() { return type; }
    public int getTargetCount() { return targetCount; }
    public int getRewardCoins() { return rewardCoins; }
    public int getCurrentCount() { return currentCount; }
    public boolean isCompleted() { return completed; }
    public Set<String> getCompletedBy() { return completedBy; }
}