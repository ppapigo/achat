package com.achat.roomrank;

import org.springframework.transaction.annotation.Transactional;

import com.achat.room.Room;
import com.achat.room.RoomStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomRankService {

    private final RoomRankStore roomRankStore;
    private final RoomStore roomStore;

    // 시즌 시작일 (설정값, 나중에 SeasonStore에서 가져오기)
    private Instant currentSeasonStart = LocalDate.of(2024, 1, 1)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC);

    public RoomRankService(RoomRankStore roomRankStore, RoomStore roomStore) {
        this.roomRankStore = roomRankStore;
        this.roomStore = roomStore;
    }

    /**
     * 🏆 전체 TOP 영구방
     */
    public List<RoomRankResponse> getTopRooms(int limit) {
        return roomRankStore.getTopRanks(limit).stream()
                .map(this::toResponse)
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    /**
     * 🔥 24시간 HOT 영구방
     */
    public List<RoomRankResponse> getHotRooms(int limit) {
        return roomRankStore.getHotRanks(limit).stream()
                .map(this::toResponse)
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    /**
     * 🌱 성장중 (생성 7일 이내)
     */
    public List<RoomRankResponse> getRisingRooms(int limit) {
        return roomRankStore.getRisingRanks(limit).stream()
                .map(this::toResponse)
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    /**
     * 👥 소규모 방 (≤ 20명)
     */
    public List<RoomRankResponse> getSmallRooms(int limit) {
        return roomRankStore.getSmallRoomRanks(limit).stream()
                .map(this::toResponse)
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    /**
     * 👑 시즌 랭킹
     */
    public List<RoomRankResponse> getSeasonRooms(int limit) {
        return roomRankStore.getSeasonRanks(currentSeasonStart, limit).stream()
                .map(this::toResponse)
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }

    /**
     * 활동 기록 (메시지 전송 시 호출)
     */
    public void recordActivity(String roomId) {
        roomStore.findById(roomId)
                .filter(Room::isPermanentRoom)
                .ifPresent(room -> {
                    RoomRank rank = roomRankStore.getOrCreate(roomId);
                    rank.recordActivity();
                });
    }

    /**
     * 점수 업데이트
     */
    public void updateRoomScore(
            String roomId,
            int validMessages,
            int uniqueUsers,
            int totalMembers,
            int returnUsers,
            int reports
    ) {
        RoomRank rank = roomRankStore.getOrCreate(roomId);
        rank.updateScores(validMessages, uniqueUsers, totalMembers, returnUsers, reports);
    }

    /**
     * 신고 추가
     */
    public void addReport(String roomId) {
        RoomRank rank = roomRankStore.getOrCreate(roomId);
        rank.addReport();
    }
    public void registerRoom(String roomId) {
        roomStore.findById(roomId)
                .filter(Room::isPermanentRoom)
                .ifPresent(room -> {
                    roomRankStore.getOrCreate(roomId);
                    System.out.println("📊 영구방 랭킹 등록: " + room.getName());
                });
    }
    /**
     * 시즌 시작일 설정
     */
    public void setSeasonStart(Instant seasonStart) {
        this.currentSeasonStart = seasonStart;
    }

    private RoomRankResponse toResponse(RoomRank rank) {
        return roomStore.findById(rank.getRoomId())
                .filter(Room::isPermanentRoom)
                .map(room -> new RoomRankResponse(
                        room.getId(),
                        room.getName(),
                        rank.getFinalScore(),
                        rank.getUniqueUserCount(),
                        room.getMemberIds().size(),
                        room.getMaxMembers(),
                        room.getCreatedAt(),
                        rank.getLastActivityAt()
                ))
                .orElse(null);
    }

    public record RoomRankResponse(
            String roomId,
            String roomName,
            double score,
            int activeUsers,
            int currentMembers,
            int maxMembers,
            Instant createdAt,
            Instant lastActivityAt
    ) {}
}