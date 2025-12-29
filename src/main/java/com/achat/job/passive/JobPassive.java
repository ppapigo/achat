package com.achat.job.passive;

import com.achat.job.JobDefinition;
import com.achat.job.JobType;

public interface JobPassive {

    JobType getType();
    String getName();
    String getDescription();
    String getPassiveDescription();

    // 레벨별 패시브 효과 값 (예: 레벨 1이면 10%, 레벨 5면 50%)
    int getPassiveValue(int level);

    default JobDefinition toDefinition() {
        return new JobDefinition(getType(), getName(), getDescription(), getPassiveDescription());
    }
}