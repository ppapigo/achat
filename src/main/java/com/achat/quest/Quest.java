package com.achat.quest;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Entity
@Table(name = "chat_quests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Quest extends JpaEntity {
    @Id @Column(length = 36)
    private String id;
    private String roomId;
    @Column(unique = true, length = 36)
    private String messageId;
    private String creatorId;
    @Enumerated(EnumType.STRING)
    private QuestType type;
    private int targetCount;
    private int rewardCoins;
    private Instant createdAt;
    private Instant expiresAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_quest_participants", joinColumns = @JoinColumn(name = "quest_id"))
    @Column(name = "user_id", length = 36)
    private Set<String> completedBy = new HashSet<>();
    private int currentCount = 0;
    private boolean completed = false;
    private boolean cancelled = false;

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
        if (completed || cancelled || isExpired()) return false;
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
        return !Instant.now().isBefore(expiresAt);
    }
    public void cancel() { cancelled = true; }
}
