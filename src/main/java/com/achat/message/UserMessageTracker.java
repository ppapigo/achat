package com.achat.message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserMessageTracker {

    private final List<MessageRecord> records = new ArrayList<>();

    public record MessageRecord(Instant timestamp, int length) {}

    /**
     * 메시지 전송 가능 여부 체크
     * @return 남은 쿨타임 (ms). 0이면 전송 가능
     */
    public synchronized long checkCooldown(int messageLength) {
        cleanOldRecords();

        if (records.isEmpty()) {
            return 0;
        }

        MessageRecord lastRecord = records.get(records.size() - 1);

        // 기본 쿨타임 계산 (이전 메시지 길이 기준)
        long baseCooldown = MessageCooldownPolicy.getCooldownMs(lastRecord.length());

        // 연속 장문 가중치
        int recentLongCount = (int) records.stream()
                .filter(r -> r.length() >= MessageCooldownPolicy.LONG_MESSAGE_THRESHOLD)
                .count();

        double multiplier = MessageCooldownPolicy.getConsecutiveMultiplier(recentLongCount);
        long finalCooldown = (long) Math.min(
                baseCooldown * multiplier,
                MessageCooldownPolicy.MAX_COOLDOWN_MS
        );

        // 남은 쿨타임
        long elapsed = Instant.now().toEpochMilli() - lastRecord.timestamp().toEpochMilli();
        long remaining = finalCooldown - elapsed;

        return Math.max(0, remaining);
    }

    /**
     * 증강 배율 적용된 쿨타임 체크
     * @param cooldownMultiplier 쿨타임 배율 (예: 0.6 = 40% 감소)
     */
    public synchronized long checkCooldownWithMultiplier(int messageLength, double cooldownMultiplier) {
        cleanOldRecords();

        if (records.isEmpty()) {
            return 0;
        }

        MessageRecord lastRecord = records.get(records.size() - 1);

        // 기본 쿨타임 계산 (이전 메시지 길이 기준)
        long baseCooldown = MessageCooldownPolicy.getCooldownMs(lastRecord.length());

        // 연속 장문 가중치
        int recentLongCount = (int) records.stream()
                .filter(r -> r.length() >= MessageCooldownPolicy.LONG_MESSAGE_THRESHOLD)
                .count();

        double multiplier = MessageCooldownPolicy.getConsecutiveMultiplier(recentLongCount);

        // 증강 쿨타임 배율 적용
        long finalCooldown = (long) Math.min(
                baseCooldown * multiplier * cooldownMultiplier,
                MessageCooldownPolicy.MAX_COOLDOWN_MS
        );

        // 남은 쿨타임
        long elapsed = Instant.now().toEpochMilli() - lastRecord.timestamp().toEpochMilli();
        long remaining = finalCooldown - elapsed;

        return Math.max(0, remaining);
    }

    /**
     * 메시지 전송 기록
     */
    public synchronized void recordMessage(int messageLength) {
        records.add(new MessageRecord(Instant.now(), messageLength));

        // 최대 20개만 유지
        while (records.size() > 20) {
            records.remove(0);
        }
    }

    /**
     * 연속 장문 경고 필요 여부
     */
    public synchronized boolean shouldWarn() {
        cleanOldRecords();

        long recentLongCount = records.stream()
                .filter(r -> r.length() >= MessageCooldownPolicy.LONG_MESSAGE_THRESHOLD)
                .count();

        return recentLongCount >= 3;
    }

    /**
     * 오래된 기록 정리
     */
    private void cleanOldRecords() {
        Instant cutoff = Instant.now().minusMillis(MessageCooldownPolicy.CONSECUTIVE_WINDOW_MS);
        records.removeIf(r -> r.timestamp().isBefore(cutoff));
    }

    /**
     * 기록 초기화
     */
    public synchronized void clear() {
        records.clear();
    }
}