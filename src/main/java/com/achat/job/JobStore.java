package com.achat.job;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class JobStore {
    private final UserJobStateRepository repository;

    public UserJobState getOrCreate(String userId) { return repository.findById(userId).orElseGet(() -> repository.save(new UserJobState(userId))); }
    public Optional<UserJobState> find(String userId) { return repository.findById(userId); }
    public void save(UserJobState state) { repository.save(state); }
}
