package com.achat.friend;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "friend_requests", indexes = @Index(columnList = "to_user_id,status,created_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class FriendRequest extends JpaEntity {

    public enum Status {
        PENDING, ACCEPTED, REJECTED
    }

    @Id
    @Column(length = 36)
    private String id;
    @Column(length = 36)
    private String fromUserId;
    @Column(length = 36)
    private String toUserId;
    @Enumerated(EnumType.STRING)
    private Status status;
    private Instant createdAt;

    public FriendRequest(String fromUserId, String toUserId) {
        this.id = UUID.randomUUID().toString();
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
    }

    public void accept() { this.status = Status.ACCEPTED; }
    public void reject() { this.status = Status.REJECTED; }
}
