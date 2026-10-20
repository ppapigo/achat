package com.achat.augment;

import com.achat.persistence.JpaEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** Persisted per-recipient, per-picking-cycle limits. */
@Entity
@Table(name = "augment_progress")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AugmentProgress extends JpaEntity {
    @Id @Column(length = 100)
    private String id;
    private String roomId;
    private String roundId;
    private String userId;
    @Enumerated(EnumType.STRING)
    private AugmentType type;
    private String forbiddenWord;
    private int messagesSeen;
    private int violations;
    private int draws;
    private int wins;
    private int rewardCount;
    private int awardedPoints;
    private Instant lastDrawAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "augment_reward_content", joinColumns = @JoinColumn(name = "progress_id"))
    @Column(length = 64)
    private Set<String> contentHashes = new HashSet<>();

    public AugmentProgress(String roomId, String roundId, String userId, AugmentType type, String word) {
        this.id = key(roundId, userId);
        this.roomId = roomId;
        this.roundId = roundId;
        this.userId = userId;
        this.type = type;
        this.forbiddenWord = word;
    }
    public static String key(String roundId, String userId) { return roundId + ":" + userId; }
    public void recordMessage() { messagesSeen++; }
    public void violate() { violations++; }
    public void draw(String hash, Instant now) { draws++; lastDrawAt = now; contentHashes.add(hash); }
    public void win() { wins++; }
    public void award(int points) { awardedPoints += Math.max(0, points); rewardCount++; }
}
