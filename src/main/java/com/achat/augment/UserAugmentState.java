package com.achat.augment;

import java.time.Instant;
import java.util.List;

public class UserAugmentState {
    private final String userId;
    private final String roomId;
    private final List<AugmentType> options;
    private final Instant expiresAt;
    private AugmentType selectedAugment;

    public UserAugmentState(String userId, String roomId, List<AugmentType> options) {
        this.userId = userId;
        this.roomId = roomId;
        this.options = options;
        this.expiresAt = Instant.now().plusSeconds(300);
    }

    public String getUserId() { return userId; }
    public String getRoomId() { return roomId; }
    public List<AugmentType> getOptions() { return options; }
    public Instant getExpiresAt() { return expiresAt; }
    public AugmentType getSelectedAugment() { return selectedAugment; }

    public boolean hasSelected() {
        return selectedAugment != null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void select(AugmentType type) {
        if (isExpired()) {
            throw new IllegalStateException("선택 시간이 만료되었습니다");
        }
        if (hasSelected()) {
            throw new IllegalStateException("이미 증강을 선택했습니다");
        }
        if (!options.contains(type)) {
            throw new IllegalArgumentException("선택 가능한 증강이 아닙니다");
        }
        this.selectedAugment = type;
    }
}