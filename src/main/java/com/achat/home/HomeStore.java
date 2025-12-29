package com.achat.home;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Home 메모리 저장소
 */
@Component
public class HomeStore {

    // userId -> Home
    private final Map<String, Home> homes = new ConcurrentHashMap<>();

    /**
     * Home 저장
     */
    public void save(Home home) {
        homes.put(home.getUserId(), home);
    }

    /**
     * Home 조회 (없으면 생성)
     */
    public Home findOrCreate(String userId) {
        return homes.computeIfAbsent(userId, Home::new);
    }

    /**
     * Home 조회
     */
    public Optional<Home> findByUserId(String userId) {
        return Optional.ofNullable(homes.get(userId));
    }

    /**
     * Home 존재 여부
     */
    public boolean exists(String userId) {
        return homes.containsKey(userId);
    }

    /**
     * 방문수 증가
     */
    public void incrementVisit(String userId) {
        Home home = findOrCreate(userId);
        home.incrementVisitCount();
    }
}