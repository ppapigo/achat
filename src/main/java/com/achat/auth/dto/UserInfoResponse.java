package com.achat.auth.dto;

import com.achat.user.User;

public record UserInfoResponse(
        String id,
        String email,
        String username,
        int coins,
        int exp,
        int level,
        int expToNextLevel,
        int rp,
        String tier,
        // 배치 정보
        boolean placementCompleted,
        int placementGamesPlayed,
        int placementGamesRemaining,
        // 관리자 여부
        boolean admin
) {
    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getCoins(),
                user.getExp(),
                user.getLevel(),
                user.getExpToNextLevel(),
                user.getRp(),
                user.getTier().name(),
                user.isPlacementCompleted(),
                user.getPlacementGamesPlayed(),
                user.getPlacementGamesRemaining(),
                user.isAdmin()
        );
    }
}