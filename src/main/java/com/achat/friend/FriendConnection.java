package com.achat.friend;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.achat.persistence.JpaEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "friend_connections", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendConnection extends JpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 36)
    private String userId;
    @Column(nullable = false, length = 36)
    @Getter
    private String friendId;

    public FriendConnection(String userId, String friendId) {
        this.userId = userId;
        this.friendId = friendId;
    }
}
