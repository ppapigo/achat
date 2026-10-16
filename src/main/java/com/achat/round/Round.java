package com.achat.round;

import lombok.AccessLevel;
import lombok.Getter;

import java.time.Instant;
import java.util.*;

@Getter
public class Round {
    private final String roomId;
    private final int durationMinutes;
    @Getter(AccessLevel.NONE)
    private final int pickDurationSeconds = 20;

    private RoundPhase phase;
    private int currentPhaseNumber;
    private Instant phaseEndsAt;
    private Instant roundStartedAt;

    // 점수 시스템
    private final Map<String, RoundScore> scores = new HashMap<>();
    private final Map<String, RoundReward> rewards = new HashMap<>();
    private final RoundScoreTracker tracker = new RoundScoreTracker();

    public Round(String roomId, int durationMinutes) {
        this.roomId = roomId;
        this.durationMinutes = durationMinutes;
        this.phase = RoundPhase.WAITING;
        this.currentPhaseNumber = 0;
    }

    public int getPlayDurationSeconds() {
        int totalSeconds = durationMinutes * 60;
        int playTime = (totalSeconds - pickDurationSeconds * 3) / 3;
        return Math.max(10, playTime);
    }

    public void startRound() {
        this.roundStartedAt = Instant.now();
        startPicking();
    }

    public void startPicking() {
        this.phase = RoundPhase.PICKING;
        this.currentPhaseNumber++;
        this.phaseEndsAt = Instant.now().plusSeconds(pickDurationSeconds);
    }

    public void startPlaying() {
        this.phase = RoundPhase.PLAYING;
        this.phaseEndsAt = Instant.now().plusSeconds(getPlayDurationSeconds());
    }

    public void end() {
        this.phase = RoundPhase.ENDED;
        this.phaseEndsAt = null;
        calculateResults();
    }

    public void reset() {
        this.phase = RoundPhase.WAITING;
        this.currentPhaseNumber = 0;
        this.phaseEndsAt = null;
        this.roundStartedAt = null;
        this.scores.clear();
        this.rewards.clear();
        this.tracker.reset();
    }

    public boolean isLastPhase() {
        return currentPhaseNumber >= 3;
    }

    // === 점수 시스템 (어뷰징 방지 적용) ===

    public void addParticipant(String userId) {
        scores.putIfAbsent(userId, new RoundScore(userId));
    }

    /**
     * 메시지 처리 - 어뷰징 체크 후 점수 인정
     * @return true면 점수 인정됨, false면 0점
     */
    public boolean processMessage(String userId, String content) {
        if (phase != RoundPhase.PLAYING) {
            return false;
        }

        RoundScore score = scores.get(userId);  // ← 세미콜론 추가
        if (score == null) {
            return false;
        }

        // 어뷰징 체크
        if (!tracker.canScore(userId, content)) {
            return false;
        }

        // 점수 인정
        score.addScoredMessage();
        tracker.recordScored(userId, content);
        return true;
    }

    public void recordAugmentUsed(String userId) {
        RoundScore score = scores.get(userId);
        if (score != null) {
            score.markAugmentUsed();
        }
    }

    private void calculateResults() {
        scores.values().forEach(RoundScore::calculate);

        List<RoundScore> sorted = new ArrayList<>(scores.values());
        sorted.sort((a, b) -> b.getTotalPoints() - a.getTotalPoints());

        int rank = 1;
        for (RoundScore score : sorted) {
            score.setRank(rank);
            rewards.put(score.getUserId(), RoundReward.forRank(rank));
            rank++;
        }
    }
}
