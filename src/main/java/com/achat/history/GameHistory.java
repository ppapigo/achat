package com.achat.history;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GameHistory {
    private final String id;
    private final String odUserId;
    private final String roomId;
    private final String roomName;
    private final boolean isMatchRoom;
    private final int playerCount;
    private final int rank;           // 1등, 2등, ...
    private final int totalPoints;
    private final int messageCount;
    private final int rpChange;
    private final int coinsEarned;
    private final int expEarned;
    private final String augmentUsed;
    private final Instant playedAt;

    public GameHistory(String odUserId, String roomId, String roomName, boolean isMatchRoom,
                       int playerCount, int rank, int totalPoints, int messageCount,
                       int rpChange, int coinsEarned, int expEarned, String augmentUsed) {
        this.id = UUID.randomUUID().toString();
        this.odUserId = odUserId;
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
    public String getUserId() { return odUserId; }
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