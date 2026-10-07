package com.achat.social;

import com.achat.persistence.JpaEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_interactions", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "target_user_id"}))
public class UserInteraction extends JpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 36)
    private String userId;
    @Column(nullable = false, length = 36)
    private String targetUserId;
    private Instant lastInteractionAt;

    protected UserInteraction() {}
    public UserInteraction(String userId, String targetUserId) {
        this.userId = userId;
        this.targetUserId = targetUserId;
    }
    public void recordAt(Instant time) { lastInteractionAt = time; }
    public String getTargetUserId() { return targetUserId; }
}
