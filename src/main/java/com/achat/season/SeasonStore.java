package com.achat.season;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;

@Component
@Transactional
@RequiredArgsConstructor
public class SeasonStore {
    private final SeasonRepository repository;
    private final UserSeasonHistoryRepository historyRepository;

    public void saveSeason(Season season) { repository.save(season); }
    public Optional<Season> findById(int id) { return repository.findById(id); }
    public Season getCurrentSeason() { return repository.findFirstByActiveTrueOrderByIdDesc().orElse(null); }
    public void saveHistory(UserSeasonHistory history) { historyRepository.save(history); }
    public List<UserSeasonHistory> getUserHistory(String userId) { return historyRepository.findByUserIdOrderByRecordedAtAsc(userId); }
    public int getNextSeasonId() { return repository.findFirstByOrderByIdDesc().map(s -> s.getId() + 1).orElse(1); }
    public List<Season> getAllSeasons() { return repository.findAllByOrderByIdAsc(); }
}
