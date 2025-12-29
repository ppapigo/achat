package com.achat.sanction;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class SanctionStore {

    // id -> Sanction
    private final Map<String, Sanction> sanctions = new ConcurrentHashMap<>();

    // userId -> List<sanctionId> (빠른 조회용)
    private final Map<String, List<String>> userSanctions = new ConcurrentHashMap<>();

    public void save(Sanction sanction) {
        sanctions.put(sanction.getId(), sanction);
        userSanctions.computeIfAbsent(sanction.getUserId(), k -> new ArrayList<>())
                .add(sanction.getId());
    }

    public Optional<Sanction> findById(String id) {
        return Optional.ofNullable(sanctions.get(id));
    }

    /**
     * 특정 유저의 모든 제재 조회
     */
    public List<Sanction> findByUserId(String userId) {
        List<String> ids = userSanctions.getOrDefault(userId, List.of());
        return ids.stream()
                .map(sanctions::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Sanction::getIssuedAt).reversed())
                .collect(Collectors.toList());
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
        return sanctions.values().stream()
                .sorted(Comparator.comparing(Sanction::getIssuedAt).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 활성 제재만 조회 (관리자용)
     */
    public List<Sanction> findAllActive() {
        return sanctions.values().stream()
                .filter(Sanction::isEffective)
                .sorted(Comparator.comparing(Sanction::getIssuedAt).reversed())
                .collect(Collectors.toList());
    }
}