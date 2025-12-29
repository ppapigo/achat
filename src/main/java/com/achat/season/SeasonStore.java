package com.achat.season;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SeasonStore {

    private final Map<Integer, Season> seasons = new ConcurrentHashMap<>();
    private final Map<String, List<UserSeasonHistory>> userHistories = new ConcurrentHashMap<>();
    private Season currentSeason;

    public void saveSeason(Season season) {
        seasons.put(season.getId(), season);
        if (season.isActive()) {
            currentSeason = season;
        }
    }

    public Optional<Season> findById(int seasonId) {
        return Optional.ofNullable(seasons.get(seasonId));
    }

    public Season getCurrentSeason() {
        return currentSeason;
    }

    public void saveHistory(UserSeasonHistory history) {
        userHistories
                .computeIfAbsent(history.getUserId(), k -> new ArrayList<>())
                .add(history);
    }

    public List<UserSeasonHistory> getUserHistory(String userId) {
        return userHistories.getOrDefault(userId, List.of());
    }

    public int getNextSeasonId() {
        return seasons.size() + 1;
    }

    // 추가!
    public List<Season> getAllSeasons() {
        return new ArrayList<>(seasons.values());
    }
}