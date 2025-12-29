package com.achat.job.passive;

import com.achat.job.JobType;
import org.springframework.stereotype.Component;

@Component
public class GuardianPassive implements JobPassive {

    @Override
    public JobType getType() { return JobType.GUARDIAN; }

    @Override
    public String getName() { return "가디언"; }

    @Override
    public String getDescription() {
        return "방의 안전과 질서를 지키는 수호자";
    }

    @Override
    public String getPassiveDescription() {
        return "내가 있는 방의 스팸 필터 강도 증가";
    }

    @Override
    public int getPassiveValue(int level) {
        // 레벨당 10% 증가
        return level * 10;
    }
}