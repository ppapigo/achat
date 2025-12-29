package com.achat.job.passive;

import com.achat.job.JobType;
import org.springframework.stereotype.Component;

@Component
public class ArchitectPassive implements JobPassive {

    @Override
    public JobType getType() { return JobType.ARCHITECT; }

    @Override
    public String getName() { return "아키텍트"; }

    @Override
    public String getDescription() {
        return "방의 규칙과 구조를 설계하는 건축가";
    }

    @Override
    public String getPassiveDescription() {
        return "내가 만든 방의 커스텀 증강 슬롯 추가";
    }

    @Override
    public int getPassiveValue(int level) {
        // 레벨 2마다 슬롯 1개
        return level / 2 + 1;
    }
}