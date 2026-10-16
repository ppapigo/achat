package com.achat.roomrank;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;

/**
 * 영구방 순위 데이터
 */
@Entity
@Table(name = "room_ranks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RoomRank extends JpaEntity {

    @Id
    @Column(length = 36)
    private String roomId;

    // 개별 점수
    private int activityScore;      // 활동 점수
    private int participationScore; // 참여 점수
    private int retentionScore;     // 유지 점수
    private int penaltyScore;       // 패널티

    // 통계 데이터
    private int validMessageCount;  // 유효 메시지 수
    private int uniqueUserCount;    // 참여 유저 수
    private int totalMemberCount;   // 전체 멤버 수
    private int returnUserCount;    // 재방문 유저 수
    private int reportCount;        // 신고 수

    private Instant lastActivityAt; // 마지막 활동 시간
    private Instant updatedAt;

    public RoomRank(String roomId) {
        this.roomId = roomId;
        this.lastActivityAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // === 점수 계산 ===

    /**
     * 최종 점수 (TimeDecay 적용)
     */
    public double getFinalScore() {
        int rawScore = activityScore + participationScore + retentionScore - penaltyScore;
        return Math.max(0, rawScore * getTimeDecay());
    }

    /**
     * 시간 감쇠 (6시간 기준)
     */
    private double getTimeDecay() {
        long hoursSinceActivity = java.time.Duration.between(lastActivityAt, Instant.now()).toHours();
        return 1.0 / (1.0 + (hoursSinceActivity / 6.0));
    }

    // === 점수 업데이트 ===

    public void updateScores(
            int validMessages,
            int uniqueUsers,
            int totalMembers,
            int returnUsers,
            int reports
    ) {
        this.validMessageCount = validMessages;
        this.uniqueUserCount = uniqueUsers;
        this.totalMemberCount = totalMembers;
        this.returnUserCount = returnUsers;
        this.reportCount = reports;

        // 활동 점수: 유효 메시지 × 1 + 유니크 유저 × 10
        this.activityScore = validMessages + (uniqueUsers * 10);

        // 참여 점수: 참여율 × 50
        double participationRate = totalMembers > 0
                ? (double) uniqueUsers / totalMembers
                : 0;
        this.participationScore = (int) (participationRate * 50);

        // 유지 점수: 재방문 × 15
        this.retentionScore = returnUsers * 15;

        // 패널티: 신고 × 20
        this.penaltyScore = reports * 20;

        this.updatedAt = Instant.now();
    }

    public void recordActivity() {
        this.lastActivityAt = Instant.now();
    }

    public void addReport() {
        this.reportCount++;
        this.penaltyScore += 20;
        this.updatedAt = Instant.now();
    }
}
