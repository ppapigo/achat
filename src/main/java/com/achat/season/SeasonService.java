package com.achat.season;

import org.springframework.transaction.annotation.Transactional;

import com.achat.room.Room;
import com.achat.room.RoomStore;
import com.achat.user.User;
import com.achat.user.UserStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SeasonService {

    private final SeasonStore seasonStore;
    private final UserStore userStore;
    private final RoomStore roomStore;

    public SeasonService(SeasonStore seasonStore, UserStore userStore, RoomStore roomStore) {
        this.seasonStore = seasonStore;
        this.userStore = userStore;
        this.roomStore = roomStore;
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void initializeFirstSeason() {
        if (seasonStore.getCurrentSeason() == null) {
            Instant start = Instant.now();
            Instant end = start.plus(30, ChronoUnit.DAYS);
            int seasonId = seasonStore.getNextSeasonId();
            Season season = new Season(seasonId, "시즌 " + seasonId, start, end);
            seasonStore.saveSeason(season);
            assignSeasonToRooms(season.getId());
            System.out.println("📅 " + season.getName() + " 시작: " + start + " ~ " + end);
        }
    }

    public SeasonInfo getCurrentSeasonInfo() {
        Season season = seasonStore.getCurrentSeason();
        if (season == null) return null;

        long totalSeconds = season.getEndDate().getEpochSecond() - season.getStartDate().getEpochSecond();
        long elapsedSeconds = Instant.now().getEpochSecond() - season.getStartDate().getEpochSecond();
        int progressPercent = (int) Math.min(100, Math.max(0, (elapsedSeconds * 100) / totalSeconds));

        return new SeasonInfo(
                season.getId(), season.getName(), season.getStartDate(),
                season.getEndDate(), season.getRemainingDays(), season.isActive(), progressPercent
        );
    }

    public SeasonEndResult endCurrentSeason() {
        Season currentSeason = seasonStore.getCurrentSeason();
        if (currentSeason == null) {
            throw new IllegalStateException("활성 시즌이 없습니다");
        }

        List<UserSeasonHistory> histories = new ArrayList<>();
        int totalRewards = 0;

        for (User user : userStore.findAll()) {
            int rewardCoins = SeasonReward.getRewardCoins(user.getTier(), user.getSeasonHighestTier());
            UserSeasonHistory history = new UserSeasonHistory(
                    user.getId(), currentSeason.getId(), user.getRp(),
                    user.getTier(), user.getSeasonHighestTier(), rewardCoins
            );
            seasonStore.saveHistory(history);
            histories.add(history);
            user.addCoins(rewardCoins);
            totalRewards += rewardCoins;
            user.resetSeason(currentSeason.getId() + 1);
            userStore.save(user);
        }

        currentSeason.end();
        seasonStore.saveSeason(currentSeason);

        int newSeasonId = seasonStore.getNextSeasonId();
        Instant start = Instant.now();
        Instant end = start.plus(30, ChronoUnit.DAYS);
        Season newSeason = new Season(newSeasonId, "시즌 " + newSeasonId, start, end);
        seasonStore.saveSeason(newSeason);

        int roomsReset = resetPermanentRooms(currentSeason.getId(), newSeasonId);

        System.out.println("🎉 새 시즌 시작: " + newSeason.getName());
        return new SeasonEndResult(currentSeason.getId(), newSeasonId, histories.size(), totalRewards, roomsReset);
    }

    private int resetPermanentRooms(int oldSeasonId, int newSeasonId) {
        int count = 0;
        for (Room room : roomStore.findAll()) {
            if (room.isPermanentRoom()) {
                room.resetSeasonStats();
                room.setSeasonId(newSeasonId);
                count++;
            }
        }
        return count;
    }

    private void assignSeasonToRooms(int seasonId) {
        for (Room room : roomStore.findAll()) {
            if (room.isPermanentRoom() && room.getSeasonId() == 0) {
                room.setSeasonId(seasonId);
            }
        }
    }

    public void assignCurrentSeasonToRoom(Room room) {
        if (room.isPermanentRoom()) {
            Season current = seasonStore.getCurrentSeason();
            if (current != null) room.setSeasonId(current.getId());
        }
    }

    public List<UserSeasonHistory> getUserSeasonHistory(String userId) {
        return seasonStore.getUserHistory(userId);
    }

    public List<Season> getAllSeasons() {
        return seasonStore.getAllSeasons();
    }

    public record SeasonInfo(int seasonId, String name, Instant startDate, Instant endDate,
                             long remainingDays, boolean active, int progressPercent) {}

    public record SeasonEndResult(int endedSeasonId, int newSeasonId, int usersProcessed,
                                  int totalRewardsGiven, int roomsReset) {}
}
