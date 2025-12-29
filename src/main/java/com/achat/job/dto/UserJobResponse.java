package com.achat.job.dto;

import com.achat.job.JobType;
import com.achat.job.UserJobState;

public record UserJobResponse(
        JobType jobType,
        String jobName,
        int level,
        int exp,
        int expForNextLevel,
        int passiveValue,
        boolean isFreelancer,
        JobType recommendedJob
) {
    public static UserJobResponse from(UserJobState state, String jobName, int passiveValue) {
        return new UserJobResponse(
                state.getCurrentJob(),
                jobName,
                state.getLevel(),
                state.getExp(),
                state.getExpForNextLevel(),
                passiveValue,
                state.isFreelancer(),
                state.getRecommendedJob()
        );
    }
}