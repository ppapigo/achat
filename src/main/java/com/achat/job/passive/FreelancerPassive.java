package com.achat.job.passive;

import com.achat.job.JobType;
import org.springframework.stereotype.Component;

@Component
public class FreelancerPassive implements JobPassive {

    @Override
    public JobType getType() { return JobType.FREELANCER; }

    @Override
    public String getName() { return "자유인"; }

    @Override
    public String getDescription() {
        return "아직 길을 정하지 않은 탐험가. 모든 가능성이 열려있다.";
    }

    @Override
    public String getPassiveDescription() {
        return "증강 선택 시 추가 옵션 제공, 방 이동 제약 없음";
    }

    @Override
    public int getPassiveValue(int level) {
        // 자유인은 레벨 개념 없음, 고정 보너스
        return 1;  // 추가 증강 옵션 1개
    }
}