package com.achat.friend;

import java.time.Instant;
import java.util.UUID;

public class FriendRequest {

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }

    private final String id;
    private final String fromUserId;
    private final String toUserId;
    private Status status;
    private final Instant createdAt;

    public FriendRequest(String fromUserId, String toUserId) {
        this.id = UUID.randomUUID().toString();
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public void accept() { this.status = Status.ACCEPTED; }
    public void reject() { this.status = Status.REJECTED; }

    // Getters
    public String getId() { return id; }
    public String getFromUserId() { return fromUserId; }
    public String getToUserId() { return toUserId; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}