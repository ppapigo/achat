package com.achat.job;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

@Entity
@Table(name = "user_job_states")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class UserJobState extends JpaEntity {

    @Id
    @Column(length = 36)
    private String userId;
    @Enumerated(EnumType.STRING)
    private JobType currentJob;
    private int level;
    private int exp;

    // 전직 추천용 행동 카운터
    private int moderationActions;   // 가디언 성향
    private int roomDesignActions;   // 아키텍트 성향
    private int inviteActions;       // 커넥터 성향
    private int knowledgeActions;    // 메이븐 성향

    private static final int[] EXP_REQUIRED = {0, 100, 300, 600, 1000, 1500};

    public UserJobState(String userId) {
        this.userId = userId;
        this.currentJob = JobType.FREELANCER;  // 기본값: 자유인
        this.level = 1;
        this.exp = 0;
    }

    public boolean isFreelancer() {
        return currentJob == JobType.FREELANCER;
    }

    public void changeJob(JobType newJob) {
        if (newJob == JobType.FREELANCER) {
            throw new IllegalArgumentException("자유인으로는 전직할 수 없습니다");
        }

        // 전직 변경 시 레벨/경험치 유지
        this.currentJob = newJob;
    }

    private int calculateBonusExp(JobType job) {
        int relevantActions = switch (job) {
            case GUARDIAN -> moderationActions;
            case ARCHITECT -> roomDesignActions;
            case CONNECTOR -> inviteActions;
            case MAVEN -> knowledgeActions;
            default -> 0;
        };
        // 관련 행동 1회당 5 경험치 보너스 (최대 50)
        return Math.min(relevantActions * 5, 50);
    }

    public void addExp(int amount) {
        if (isFreelancer()) return;  // 자유인은 경험치 없음
        if (level >= 5) return;

        this.exp += amount;
        checkLevelUp();
    }

    private void checkLevelUp() {
        while (level < 5 && exp >= getExpForNextLevel()) {
            exp -= getExpForNextLevel();
            level++;
        }
    }

    public int getExpForNextLevel() {
        if (level >= 5) return 0;
        return EXP_REQUIRED[level + 1] - EXP_REQUIRED[level];
    }

    // 행동 기록 (전직 추천용)
    public void recordAction(ActionType type) {
        switch (type) {
            case MODERATION -> moderationActions++;
            case ROOM_DESIGN -> roomDesignActions++;
            case INVITE -> inviteActions++;
            case KNOWLEDGE -> knowledgeActions++;
        }
    }

    public JobType getRecommendedJob() {
        int max = Math.max(Math.max(moderationActions, roomDesignActions),
                Math.max(inviteActions, knowledgeActions));

        if (max < 5) return null;  // 최소 5회 이상 행동해야 추천

        if (moderationActions == max) return JobType.GUARDIAN;
        if (roomDesignActions == max) return JobType.ARCHITECT;
        if (inviteActions == max) return JobType.CONNECTOR;
        if (knowledgeActions == max) return JobType.MAVEN;

        return null;
    }

    public enum ActionType {
        MODERATION,   // 신고, 경고 등
        ROOM_DESIGN,  // 방 생성, 설정 변경
        INVITE,       // 초대
        KNOWLEDGE     // 답변, 정보 공유
    }
}
