package com.achat.job;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.job.dto.JobResponse;
import com.achat.job.dto.SelectJobRequest;
import com.achat.job.dto.UserJobResponse;
import com.achat.job.passive.JobPassiveRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class JobService {

    private final JobStore jobStore;
    private final JobPassiveRegistry passiveRegistry;

    public List<JobResponse> getAllJobs() {
        return passiveRegistry.getAllDefinitions().stream()
                .map(JobResponse::from)
                .toList();
    }

    public List<JobResponse> getSelectableJobs() {
        // 자유인 제외한 전직 목록
        return passiveRegistry.getAllDefinitions().stream()
                .filter(def -> def.type() != JobType.FREELANCER)
                .map(JobResponse::from)
                .toList();
    }

    public UserJobResponse getMyJob(String userId) {
        UserJobState state = jobStore.getOrCreate(userId);

        String jobName = passiveRegistry.getDefinition(state.getCurrentJob()).name();
        int passiveValue = passiveRegistry.getPassiveValue(state.getCurrentJob(), state.getLevel());

        return UserJobResponse.from(state, jobName, passiveValue);
    }

    public UserJobResponse selectJob(String userId, SelectJobRequest request) {
        UserJobState state = jobStore.getOrCreate(userId);

        JobType newJob = JobType.valueOf(request.jobType());

        // 같은 전직이면 그냥 현재 상태 반환
        if (state.getCurrentJob() == newJob) {
            String jobName = passiveRegistry.getDefinition(state.getCurrentJob()).name();
            int passiveValue = passiveRegistry.getPassiveValue(state.getCurrentJob(), state.getLevel());
            return UserJobResponse.from(state, jobName, passiveValue);
        }

        // 전직 존재 확인
        passiveRegistry.getDefinition(newJob);

        state.changeJob(newJob);
        jobStore.save(state);

        String jobName = passiveRegistry.getDefinition(state.getCurrentJob()).name();
        int passiveValue = passiveRegistry.getPassiveValue(state.getCurrentJob(), state.getLevel());

        return UserJobResponse.from(state, jobName, passiveValue);
    }

    public UserJobResponse addExp(String userId, int amount) {
        UserJobState state = jobStore.getOrCreate(userId);

        if (state.isFreelancer()) {
            throw new IllegalStateException("자유인은 경험치를 얻을 수 없습니다. 먼저 전직하세요.");
        }

        state.addExp(amount);
        jobStore.save(state);

        String jobName = passiveRegistry.getDefinition(state.getCurrentJob()).name();
        int passiveValue = passiveRegistry.getPassiveValue(state.getCurrentJob(), state.getLevel());

        return UserJobResponse.from(state, jobName, passiveValue);
    }

    public void recordAction(String userId, UserJobState.ActionType actionType) {
        UserJobState state = jobStore.getOrCreate(userId);
        state.recordAction(actionType);
        jobStore.save(state);
    }

    public int getPassiveValue(String userId) {
        UserJobState state = jobStore.getOrCreate(userId);
        return passiveRegistry.getPassiveValue(state.getCurrentJob(), state.getLevel());
    }

    public boolean isFreelancer(String userId) {
        return jobStore.getOrCreate(userId).isFreelancer();
    }
}
