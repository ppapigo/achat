package com.achat.round;

public class RoundScore {
    private final String userId;  // 필드명 수정
    private int scoredMessageCount;
    private boolean augmentUsed;
    private int totalPoints;
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
        this.totalPoints =
                (scoredMessageCount * ScorePolicy.POINTS_PER_MESSAGE)
                        + (augmentUsed ? ScorePolicy.AUGMENT_BONUS : 0);
    }

    // Getters
    public String getUserId() { return userId; }
    public int getScoredMessageCount() { return scoredMessageCount; }
    public int getMessageCount() { return scoredMessageCount; }  // 호환용 추가
    public boolean isAugmentUsed() { return augmentUsed; }
    public int getTotalPoints() { return totalPoints; }
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}