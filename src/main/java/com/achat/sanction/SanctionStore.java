package com.achat.sanction;

import org.springframework.stereotype.Component;

import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Component
@Transactional
@RequiredArgsConstructor
public class SanctionStore {

    private final SanctionRepository repository;

    public void save(Sanction sanction) { repository.save(sanction); }

    public Optional<Sanction> findById(String id) {
        return repository.findById(id);
    }

    /**
     * 특정 유저의 모든 제재 조회
     */
    public List<Sanction> findByUserId(String userId) {
        return repository.findByUserIdOrderByIssuedAtDesc(userId);
    }

    /**
     * 특정 유저의 활성 제재 조회
     */
    public List<Sanction> findActiveByUserId(String userId) {
        return findByUserId(userId).stream()
                .filter(Sanction::isEffective)
                .collect(Collectors.toList());
    }

    /**
     * 특정 유저가 특정 타입의 활성 제재가 있는지
     */
    public boolean hasActiveSanction(String userId, SanctionType type) {
        return findActiveByUserId(userId).stream()
                .anyMatch(s -> s.getType() == type);
    }

    /**
     * 채팅 금지 상태인지 체크 (MUTE 또는 BAN)
     */
    public boolean isMuted(String userId) {
        return findActiveByUserId(userId).stream()
                .anyMatch(s -> s.getType() == SanctionType.MUTE ||
                        s.getType() == SanctionType.TEMP_BAN ||
                        s.getType() == SanctionType.PERM_BAN);
    }

    /**
     * 접속 정지 상태인지 체크
     */
    public boolean isBanned(String userId) {
        return findActiveByUserId(userId).stream()
                .anyMatch(s -> s.getType() == SanctionType.TEMP_BAN ||
                        s.getType() == SanctionType.PERM_BAN);
    }

    /**
     * 모든 제재 조회 (관리자용)
     */
    public List<Sanction> findAll() {
        return repository.findAllByOrderByIssuedAtDesc().stream()
                .sorted(Comparator.comparing(Sanction::getIssuedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 활성 제재만 조회 (관리자용)
     */
    public List<Sanction> findAllActive() {
        return repository.findAllByOrderByIssuedAtDesc().stream()
                .filter(Sanction::isEffective)
                .sorted(Comparator.comparing(Sanction::getIssuedAt).reversed())
                .collect(Collectors.toList());
    }
}