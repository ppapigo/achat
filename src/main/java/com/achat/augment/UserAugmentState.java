package com.achat.augment;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class UserAugmentState {
    private final String userId;
    private final String roomId;
    private final List<AugmentType> options;
    private final Instant expiresAt;
    private AugmentType selectedAugment;
    private String targetUserId;

    public UserAugmentState(String userId, String roomId, List<AugmentType> options) {
        this.userId = userId;
        this.roomId = roomId;
        this.options = options;
        this.expiresAt = Instant.now().plusSeconds(300);
    }

    public boolean hasSelected() {
        return selectedAugment != null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public synchronized void select(AugmentType type, String targetUserId) {
        if (isExpired()) {
            throw new IllegalStateException("선택 시간이 만료되었습니다");
        }
        if (hasSelected()) {
            throw new IllegalStateException("이미 증강을 선택했습니다");
        }
        if (!options.contains(type)) {
            throw new IllegalArgumentException("선택 가능한 증강이 아닙니다");
        }
        if (targetUserId == null || targetUserId.isBlank() || userId.equals(targetUserId)) {
            throw new IllegalArgumentException("증강을 적용할 상대를 선택해주세요");
        }
        this.targetUserId = targetUserId;
        this.selectedAugment = type;
    }
}
