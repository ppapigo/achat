package com.achat.round;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 라운드별 점수 어뷰징 방지 상태 추적
 */
public class RoundScoreTracker {

    // 유저별 마지막 점수 인정 시각
    private final Map<String, Instant> lastScoredAt = new HashMap<>();

    // 유저별 점수 인정된 메시지 수
    private final Map<String, Integer> scoredCount = new HashMap<>();

    // 유저별 연속 발언 횟수
    private final Map<String, Integer> consecutiveCount = new HashMap<>();

    // 마지막 발언자 (상호작용 체크용)
    private String lastSpeaker = null;

    // 유저별 마지막 메시지 (중복 체크용)
    private final Map<String, String> lastMessage = new HashMap<>();

    /**
     * 메시지가 점수 인정 조건을 통과하는지 검사
     * @return true면 점수 인정, false면 0점
     */
    public boolean canScore(String userId, String content) {
        String trimmed = content != null ? content.trim() : "";

        // 1. 최소 길이 체크
        if (trimmed.length() < ScorePolicy.MIN_MESSAGE_LENGTH) {
            updateSpeaker(userId, trimmed);  // 발언은 기록
            return false;
        }

        // 2. 중복 메시지 체크 (직전과 동일하면 0점)
        String lastMsg = lastMessage.get(userId);
        if (lastMsg != null && lastMsg.equals(trimmed)) {
            updateSpeaker(userId, trimmed);
            return false;
        }

        // 3. 라운드당 상한 체크
        int count = scoredCount.getOrDefault(userId, 0);
        if (count >= ScorePolicy.MAX_SCORED_MESSAGES) {
            updateSpeaker(userId, trimmed);
            return false;
        }

        // 4. 레이트 리밋 체크 (5초에 1회)
        Instant lastScored = lastScoredAt.get(userId);
        if (lastScored != null) {
            long secondsSince = Instant.now().getEpochSecond() - lastScored.getEpochSecond();
            if (secondsSince < ScorePolicy.RATE_LIMIT_SECONDS) {
                updateSpeaker(userId, trimmed);
                return false;
            }
        }

        // 5. 연속 발언 체크 (2개까지만)
        if (userId.equals(lastSpeaker)) {
            int consecutive = consecutiveCount.getOrDefault(userId, 0);
            if (consecutive >= ScorePolicy.MAX_CONSECUTIVE) {
                updateSpeaker(userId, trimmed);
                return false;
            }
        }

        // 모든 조건 통과 → 점수 인정
        return true;
    }

    /**
     * 점수 인정 후 상태 업데이트
     */
    public void recordScored(String odUserId, String content) {
        String trimmed = content.trim();
        Instant now = Instant.now();

        // 레이트 리밋 갱신
        lastScoredAt.put(odUserId, now);

        // 점수 인정 카운트 증가
        scoredCount.merge(odUserId, 1, Integer::sum);

        // 연속 발언 처리
        if (odUserId.equals(lastSpeaker)) {
            consecutiveCount.merge(odUserId, 1, Integer::sum);
        } else {
            // 다른 유저가 말했으니 이전 유저 연속 카운트 리셋
            if (lastSpeaker != null) {
                consecutiveCount.put(lastSpeaker, 0);
            }
            consecutiveCount.put(odUserId, 1);
        }

        lastSpeaker = odUserId;
        lastMessage.put(odUserId, trimmed);
    }

    /**
     * 점수 불인정이어도 발언자 기록은 업데이트
     */
    private void updateSpeaker(String userId, String content) {
        // 연속 발언 카운트만 업데이트 (점수 인정 안 해도 발언은 함)
        if (userId.equals(lastSpeaker)) {
            consecutiveCount.merge(userId, 1, Integer::sum);
        } else {
            if (lastSpeaker != null) {
                consecutiveCount.put(lastSpeaker, 0);
            }
            consecutiveCount.put(userId, 1);
            lastSpeaker = userId;
        }
        lastMessage.put(userId, content);
    }

    /**
     * 유저별 점수 인정된 메시지 수 조회
     */
    public int getScoredCount(String odUserId) {
        return scoredCount.getOrDefault(odUserId, 0);
    }

    /**
     * 리셋 (라운드 종료 시)
     */
    public void reset() {
        lastScoredAt.clear();
        scoredCount.clear();
        consecutiveCount.clear();
        lastSpeaker = null;
        lastMessage.clear();
    }

    Runnable checkpoint() {
        var times = new HashMap<>(lastScoredAt);
        var counts = new HashMap<>(scoredCount);
        var consecutive = new HashMap<>(consecutiveCount);
        var messages = new HashMap<>(lastMessage);
        String speaker = lastSpeaker;
        return () -> {
            reset();
            lastScoredAt.putAll(times);
            scoredCount.putAll(counts);
            consecutiveCount.putAll(consecutive);
            lastMessage.putAll(messages);
            lastSpeaker = speaker;
        };
    }
}
