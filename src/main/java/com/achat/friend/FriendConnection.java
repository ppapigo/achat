package com.achat.friend;

import com.achat.persistence.JpaEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "friend_connections", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "friend_id"}))
public class FriendConnection extends JpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 36)
    private String userId;
    @Column(nullable = false, length = 36)
    private String friendId;

    protected FriendConnection() {}
    public FriendConnection(String userId, String friendId) {
        this.userId = userId;
        this.friendId = friendId;
    }
    public String getFriendId() { return friendId; }
}
