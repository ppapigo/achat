package com.achat.social;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.achat.persistence.JpaEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_interactions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInteraction extends JpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 36)
    private String userId;
    @Column(nullable = false, length = 36)
    @Getter
    private String targetUserId;
    private Instant lastInteractionAt;

    public UserInteraction(String userId, String targetUserId) {
        this.userId = userId;
        this.targetUserId = targetUserId;
    }
    public void recordAt(Instant time) { lastInteractionAt = time; }
}
