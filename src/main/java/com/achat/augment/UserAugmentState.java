package com.achat.augment;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class UserAugmentState {
    private final String userId;
    private final String roomId;
    private final String roundId;
    private final List<AugmentType> options;
    private final Instant expiresAt;
    private AugmentType selectedAugment;
    private String targetUserId;

    public UserAugmentState(String userId, String roomId, String roundId, List<AugmentType> options, Instant expiresAt) {
        this.userId = userId;
        this.roomId = roomId;
        this.roundId = roundId;
        this.options = List.copyOf(options);
        this.expiresAt = expiresAt;
    }

    public boolean hasSelected() {
        return selectedAugment != null;
    }

    public boolean isExpired() {
        return !Instant.now().isBefore(expiresAt);
    }

    // Only AugmentStore selects while holding the room lock, including deadline auto-selection.
    void select(AugmentType type, String targetUserId) {
        if (hasSelected()) {
            throw new AugmentConflictException("이미 증강을 선택했습니다");
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

    void undoSelection() {
        this.selectedAugment = null;
        this.targetUserId = null;
    }
}
