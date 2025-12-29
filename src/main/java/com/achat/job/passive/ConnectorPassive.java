package com.achat.job.passive;

import com.achat.job.JobType;
import org.springframework.stereotype.Component;

@Component
public class ConnectorPassive implements JobPassive {

    @Override
    public JobType getType() { return JobType.CONNECTOR; }

    @Override
    public String getName() { return "커넥터"; }

    @Override
    public String getDescription() {
        return "사람들을 연결하고 커뮤니티를 성장시키는 연결자";
    }

    @Override
    public String getPassiveDescription() {
        return "초대한 유저의 활동에 따라 보너스 경험치 획득";
    }

    @Override
    public int getPassiveValue(int level) {
        // 레벨당 5% 보너스
        return level * 5;
    }
}