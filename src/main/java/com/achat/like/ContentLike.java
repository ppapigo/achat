package com.achat.like;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import com.achat.persistence.JpaEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "content_likes", uniqueConstraints = @UniqueConstraint(columnNames = {"target_type", "target_id", "user_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentLike extends JpaEntity {
    public enum TargetType { POST, BOARD, MINI_HOME }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TargetType targetType;
    @Column(nullable = false, length = 36)
    private String targetId;
    @Column(nullable = false, length = 36)
    private String userId;

    public ContentLike(TargetType targetType, String targetId, String userId) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.userId = userId;
    }
}
