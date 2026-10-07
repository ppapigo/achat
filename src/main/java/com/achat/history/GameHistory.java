package com.achat.history;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_histories", indexes = @Index(columnList = "user_id,played_at"))
public class GameHistory extends JpaEntity {
    protected GameHistory() {}

    @Id
    @Column(length = 36)
    private String id;
    @Column(length = 36)
    private String userId;
    @Column(length = 36)
    private String roomId;
    private String roomName;
    private boolean isMatchRoom;
    private int playerCount;
    @Column(name = "finish_rank")
    private int rank;           // 1등, 2등, ...
    private int totalPoints;
    private int messageCount;
    private int rpChange;
    private int coinsEarned;
    private int expEarned;
    private String augmentUsed;
    private Instant playedAt;

    public GameHistory(String userId, String roomId, String roomName, boolean isMatchRoom,
                       int playerCount, int rank, int totalPoints, int messageCount,
                       int rpChange, int coinsEarned, int expEarned, String augmentUsed) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.roomId = roomId;
        this.roomName = roomName;
        this.isMatchRoom = isMatchRoom;
        this.playerCount = playerCount;
        this.rank = rank;
        this.totalPoints = totalPoints;
        this.messageCount = messageCount;
        this.rpChange = rpChange;
        this.coinsEarned = coinsEarned;
        this.expEarned = expEarned;
        this.augmentUsed = augmentUsed;
        this.playedAt = Instant.now();
    }

    // Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public boolean isMatchRoom() { return isMatchRoom; }
    public int getPlayerCount() { return playerCount; }
    public int getRank() { return rank; }
    public int getTotalPoints() { return totalPoints; }
    public int getMessageCount() { return messageCount; }
    public int getRpChange() { return rpChange; }
    public int getCoinsEarned() { return coinsEarned; }
    public int getExpEarned() { return expEarned; }
    public String getAugmentUsed() { return augmentUsed; }
    public Instant getPlayedAt() { return playedAt; }

    // 1등인지
    public boolean isWin() {
        return rank == 1;
    }

    // 상위 절반인지 (선방)
    public boolean isTopHalf() {
        return rank <= (playerCount / 2);
    }
}