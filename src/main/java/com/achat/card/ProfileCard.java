package com.achat.card;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "profile_cards", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"owner_id", "drawn_date"}),
    @UniqueConstraint(columnNames = {"owner_id", "target_id"})
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProfileCard extends JpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 36)
    private String ownerId;      // 카드를 뽑은 유저
    @Column(length = 36)
    private String targetId;     // 카드에 표시된 유저
    private LocalDate drawnDate; // 뽑은 날짜
    private LocalDateTime createdAt;

    public ProfileCard(Long id, String ownerId, String targetId) {
        this.id = id;
        this.ownerId = ownerId;
        this.targetId = targetId;
        this.drawnDate = LocalDate.now();
        this.createdAt = LocalDateTime.now();
    }
}
