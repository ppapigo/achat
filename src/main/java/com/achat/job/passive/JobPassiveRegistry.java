package com.achat.job.passive;

import lombok.extern.slf4j.Slf4j;

import com.achat.job.JobDefinition;
import com.achat.job.JobType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JobPassiveRegistry {

    private final Map<JobType, JobPassive> passives;

    public JobPassiveRegistry(List<JobPassive> passiveList) {
        this.passives = passiveList.stream()
                .collect(Collectors.toMap(
                        JobPassive::getType,
                        Function.identity(),
                        (a, b) -> {
                            log.warn("중복 전직 패시브: {} - 기존 유지", a.getType());
                            return a;
                        }
                ));

        log.info("전직 {}개 등록: {}", passives.size(), passives.keySet());
    }

    public JobPassive get(JobType type) {
        return passives.get(type);
    }

    public JobDefinition getDefinition(JobType type) {
        JobPassive passive = passives.get(type);
        if (passive == null) {
            throw new IllegalArgumentException("등록되지 않은 전직: " + type);
        }
        return passive.toDefinition();
    }

    public List<JobDefinition> getAllDefinitions() {
        return passives.values().stream()
                .map(JobPassive::toDefinition)
                .toList();
    }

    public int getPassiveValue(JobType type, int level) {
        JobPassive passive = passives.get(type);
        return passive != null ? passive.getPassiveValue(level) : 0;
    }
}
