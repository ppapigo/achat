package com.achat.home;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class HomeStore {
    private final HomeRepository repository;

    public void save(Home home) { repository.save(home); }
    public Home findOrCreate(String userId) { return repository.findById(userId).orElseGet(() -> repository.save(new Home(userId))); }
    public Optional<Home> findByUserId(String userId) { return repository.findById(userId); }
    public boolean exists(String userId) { return repository.existsById(userId); }
    public void incrementVisit(String userId) { findOrCreate(userId).incrementVisitCount(); }
}
