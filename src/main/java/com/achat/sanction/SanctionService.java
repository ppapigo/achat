package com.achat.sanction;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.user.UserStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SanctionService {

    private final SanctionStore sanctionStore;
    private final UserStore userStore;

    /**
     * 제재 적용
     */
    public Sanction applySanction(String targetUserId, SanctionType type,
                                  String reason, String reportId,
                                  String adminId, Integer durationMinutes) {
        // 대상 유저 존재 확인 (SYSTEM인 경우 스킵)
        if (!"SYSTEM".equals(adminId)) {
            requireAdmin(adminId);
            userStore.findById(targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("대상 유저를 찾을 수 없습니다"));
        }
        if (reason == null || reason.isBlank() || reason.length() > 1000) throw new IllegalArgumentException("제재 사유는 1~1000자여야 합니다");
        if (durationMinutes != null && (durationMinutes < -1 || durationMinutes > 525600))
            throw new IllegalArgumentException("제재 기간이 유효하지 않습니다");

        Sanction sanction = new Sanction(targetUserId, type, reason,
                reportId, adminId, durationMinutes);
        sanctionStore.save(sanction);

        System.out.println("⚖️ 제재 적용: " + targetUserId);
        System.out.println("   유형: " + type.getDescription());

        return sanction;
    }

    /**
     * 제재 해제
     */
    public Sanction revokeSanction(String sanctionId, String adminId) {
        requireAdmin(adminId);
        Sanction sanction = sanctionStore.findById(sanctionId)
                .orElseThrow(() -> new IllegalArgumentException("제재를 찾을 수 없습니다"));

        sanction.revoke();

        System.out.println("⚖️ 제재 해제: " + sanction.getUserId() + " by " + adminId);

        return sanction;
    }

    /**
     * 유저의 제재 이력 조회
     */
    public List<Sanction> getUserSanctions(String userId) {
        return sanctionStore.findByUserId(userId);
    }

    /**
     * 유저의 활성 제재 조회
     */
    public List<Sanction> getActiveSanctions(String userId) {
        return sanctionStore.findActiveByUserId(userId);
    }

    /**
     * 특정 타입의 활성 제재가 있는지 체크
     */
    public boolean hasActiveSanctionType(String userId, SanctionType type) {
        return sanctionStore.hasActiveSanction(userId, type);
    }

    /**
     * 채팅 가능 여부 체크
     */
    public boolean canChat(String userId) {
        return !sanctionStore.isMuted(userId);
    }

    /**
     * 접속 가능 여부 체크 (정지 상태)
     */
    public boolean isBanned(String userId) {
        return sanctionStore.isBanned(userId);
    }

    /**
     * 접속 가능 여부 체크
     */
    public boolean canAccess(String userId) {
        return !sanctionStore.isBanned(userId);
    }

    /**
     * 전체 제재 목록 (관리자용)
     */
    public List<Sanction> getAllSanctions() {
        return sanctionStore.findAll();
    }

    /**
     * 활성 제재만 (관리자용)
     */
    public List<Sanction> getActiveSanctionsAll() {
        return sanctionStore.findAllActive();
    }

    /**
     * 제재 타입 목록
     */
    public List<SanctionTypeInfo> getSanctionTypes() {
        return List.of(
                new SanctionTypeInfo(SanctionType.WARNING, SanctionType.WARNING.getDescription(), 0),
                new SanctionTypeInfo(SanctionType.MUTE, SanctionType.MUTE.getDescription(), 60),
                new SanctionTypeInfo(SanctionType.TEMP_BAN, SanctionType.TEMP_BAN.getDescription(), 1440),
                new SanctionTypeInfo(SanctionType.PERM_BAN, SanctionType.PERM_BAN.getDescription(), -1)
        );
    }

    public record SanctionTypeInfo(SanctionType type, String description, int defaultMinutes) {}
    private void requireAdmin(String adminId) {
        if (userStore.findById(adminId).filter(user -> user.isAdmin()).isEmpty() || isBanned(adminId))
            throw new org.springframework.security.access.AccessDeniedException("관리자 권한이 필요합니다");
    }
}
