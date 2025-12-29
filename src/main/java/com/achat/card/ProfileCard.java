package com.achat.card;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ProfileCard {
    private Long id;
    private String ownerId;      // 카드를 뽑은 유저
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

    public Long getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getTargetId() { return targetId; }
    public LocalDate getDrawnDate() { return drawnDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}