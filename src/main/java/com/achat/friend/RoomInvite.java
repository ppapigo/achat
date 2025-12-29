package com.achat.friend;

import java.time.Instant;
import java.util.UUID;

public class RoomInvite {

    public enum Status {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }

    private final String id;
    private final String fromUserId;
    private final String toUserId;
    private final String roomId;
    private Status status;
    private final Instant createdAt;
    private final Instant expiresAt;

    public RoomInvite(String fromUserId, String toUserId, String roomId) {
        this.id = UUID.randomUUID().toString();
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.roomId = roomId;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(300); // 5분
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void accept() { this.status = Status.ACCEPTED; }
    public void reject() { this.status = Status.REJECTED; }
    public void expire() { this.status = Status.EXPIRED; }

    // Getters
    public String getId() { return id; }
    public String getFromUserId() { return fromUserId; }
    public String getToUserId() { return toUserId; }
    public String getRoomId() { return roomId; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}