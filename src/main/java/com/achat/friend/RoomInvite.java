package com.achat.friend;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_invites", indexes = @Index(columnList = "to_user_id,status,expires_at"))
public class RoomInvite extends JpaEntity {
    protected RoomInvite() {}


    public enum Status {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }

    @Id
    @Column(length = 36)
    private String id;
    @Column(length = 36)
    private String fromUserId;
    @Column(length = 36)
    private String toUserId;
    @Column(length = 36)
    private String roomId;
    @Enumerated(EnumType.STRING)
    private Status status;
    private Instant createdAt;
    private Instant expiresAt;

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