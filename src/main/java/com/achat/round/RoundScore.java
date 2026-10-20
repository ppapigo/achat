package com.achat.round;

import lombok.Getter;
import lombok.Setter;

@Getter
public class RoundScore {
    private final String userId;  // 필드명 수정
    private int scoredMessageCount;
    private boolean augmentUsed;
    private int totalPoints;
    @Setter
    private int effectPoints;
    @Setter
    private int rank;

    public RoundScore(String userId) {
        this.userId = userId;  // 수정
        this.scoredMessageCount = 0;
        this.augmentUsed = false;
        this.totalPoints = 0;
        this.rank = 0;
    }

    public void addScoredMessage() {
        this.scoredMessageCount++;
    }

    public void markAugmentUsed() {
        this.augmentUsed = true;
    }

    public void calculate() {
        this.totalPoints = Math.max(0,
                (scoredMessageCount * ScorePolicy.POINTS_PER_MESSAGE)
                        + (augmentUsed ? ScorePolicy.AUGMENT_BONUS : 0) + effectPoints);
    }

    public int getMessageCount() { return scoredMessageCount; }  // 호환용 추가

    RoundScore copy() {
        var copy = new RoundScore(userId);
        copy.scoredMessageCount = scoredMessageCount;
        copy.augmentUsed = augmentUsed;
        copy.totalPoints = totalPoints;
        copy.effectPoints = effectPoints;
        copy.rank = rank;
        return copy;
    }
}
