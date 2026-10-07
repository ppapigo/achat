package com.achat.roomrank;

import com.achat.room.Room;
import com.achat.room.RoomStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Component
@Transactional
public class RoomRankStore {

    private final RoomRankRepository repository;
    private final RoomStore roomStore;

    public RoomRankStore(RoomStore roomStore, RoomRankRepository repository) {
        this.repository = repository;
        this.roomStore = roomStore;
    }

    public RoomRank getOrCreate(String roomId) {
        return repository.findById(roomId).orElseGet(() -> repository.save(new RoomRank(roomId)));
    }

    public Optional<RoomRank> findByRoomId(String roomId) {
        return repository.findById(roomId);
    }

    public void save(RoomRank rank) {
        repository.save(rank);
    }

    /**
     * 🏆 전체 TOP
     */
    public List<RoomRank> getTopRanks(int limit) {
        return getPermanentRooms()
                .sorted(Comparator.comparingDouble(RoomRank::getFinalScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 🔥 24시간 HOT
     */
    public List<RoomRank> getHotRanks(int limit) {
        return getPermanentRooms()
                .filter(r -> isWithinHours(r.getLastActivityAt(), 24))
                .sorted(Comparator.comparingDouble(RoomRank::getFinalScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 🌱 성장중 (생성 7일 이내)
     */
    public List<RoomRank> getRisingRanks(int limit) {
        return getPermanentRoomsWithRoom()
                .filter(pair -> isWithinDays(pair.room.getCreatedAt(), 7))
                .map(pair -> pair.rank)
                .sorted(Comparator.comparingDouble(RoomRank::getFinalScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 👥 소규모 방 (≤ 20명)
     */
    public List<RoomRank> getSmallRoomRanks(int limit) {
        return getPermanentRoomsWithRoom()
                .filter(pair -> pair.room.getMemberIds().size() <= 20)
                .map(pair -> pair.rank)
                .sorted(Comparator.comparingDouble(RoomRank::getFinalScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 👑 시즌 랭킹 (시즌 시작 이후 활동 기준)
     */
    public List<RoomRank> getSeasonRanks(Instant seasonStart, int limit) {
        return getPermanentRooms()
                .filter(r -> r.getLastActivityAt() != null && r.getLastActivityAt().isAfter(seasonStart))
                .sorted(Comparator.comparingDouble(RoomRank::getFinalScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // === Helper 메서드 ===

    private java.util.stream.Stream<RoomRank> getPermanentRooms() {
        return repository.findAll().stream()
                .filter(this::isPermanentRoom);
    }

    private java.util.stream.Stream<RoomRankPair> getPermanentRoomsWithRoom() {
        return repository.findAll().stream()
                .map(rank -> roomStore.findById(rank.getRoomId())
                        .filter(Room::isPermanentRoom)
                        .map(room -> new RoomRankPair(rank, room))
                        .orElse(null))
                .filter(Objects::nonNull);
    }

    private boolean isPermanentRoom(RoomRank rank) {
        return roomStore.findById(rank.getRoomId())
                .map(Room::isPermanentRoom)
                .orElse(false);
    }

    private boolean isWithinHours(Instant time, int hours) {
        return time != null &&
                time.isAfter(Instant.now().minus(hours, ChronoUnit.HOURS));
    }

    private boolean isWithinDays(Instant time, int days) {
        return time != null &&
                time.isAfter(Instant.now().minus(days, ChronoUnit.DAYS));
    }

    private record RoomRankPair(RoomRank rank, Room room) {}
}