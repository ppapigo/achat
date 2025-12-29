package com.achat.job.passive;

import com.achat.job.JobType;
import org.springframework.stereotype.Component;

@Component
public class MavenPassive implements JobPassive {

    @Override
    public JobType getType() { return JobType.MAVEN; }

    @Override
    public String getName() { return "메이븐"; }

    @Override
    public String getDescription() {
        return "지식을 나누고 대화의 품질을 높이는 현자";
    }

    @Override
    public String getPassiveDescription() {
        return "내 메시지가 인용될 때 추가 경험치 획득";
    }

    @Override
    public int getPassiveValue(int level) {
        // 레벨당 15% 추가 경험치
        return level * 15;
    }
}