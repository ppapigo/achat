package com.achat.room;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Room {
    private final String id;
    private final String name;
    private String ownerId;
    private final int maxMembers;
    private final Set<String> memberIds;
    private final Instant createdAt;
    private RoomType type = RoomType.PERMANENT;

    // 시즌 연결 (영구방용)
    private int seasonId = 0;

    // 시즌별 통계 (시즌 종료 시 리셋)
    private int seasonMessageCount = 0;
    private int seasonActiveMembers = 0;

    // 계층 구조
    private String parentId = null;
    private final Set<String> subRoomIds = new HashSet<>();

    public Room(String name, String ownerId, int maxMembers) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.ownerId = ownerId;
        this.maxMembers = maxMembers;
        this.memberIds = new HashSet<>();
        if (!ownerId.equals("SYSTEM")) {
            this.memberIds.add(ownerId);
        }
        this.createdAt = Instant.now();
    }

    public Room(String name, String ownerId, int maxMembers, RoomType type) {
        this(name, ownerId, maxMembers);
        this.type = type;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getOwnerId() { return ownerId; }
    public int getMaxMembers() { return maxMembers; }
    public Set<String> getMemberIds() { return memberIds; }
    public Instant getCreatedAt() { return createdAt; }
    public RoomType getType() { return type; }

    // 시즌 관련
    public int getSeasonId() { return seasonId; }
    public void setSeasonId(int seasonId) { this.seasonId = seasonId; }
    public int getSeasonMessageCount() { return seasonMessageCount; }
    public void addSeasonMessage() { this.seasonMessageCount++; }
    public int getSeasonActiveMembers() { return seasonActiveMembers; }
    public void setSeasonActiveMembers(int count) { this.seasonActiveMembers = count; }

    public void resetSeasonStats() {
        this.seasonMessageCount = 0;
        this.seasonActiveMembers = 0;
    }

    // 타입 체크
    public void setType(RoomType type) { this.type = type; }
    public boolean isMatchRoom() { return type == RoomType.MATCH; }
    public boolean isPartyRoom() { return type == RoomType.PARTY; }
    public boolean isPermanentRoom() { return type == RoomType.PERMANENT; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    // 계층 구조
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public Set<String> getSubRoomIds() { return subRoomIds; }
    public void addSubRoom(String subRoomId) { subRoomIds.add(subRoomId); }
    public void removeSubRoom(String subRoomId) { subRoomIds.remove(subRoomId); }
    public boolean isRootRoom() { return parentId == null; }
    public boolean hasSubRooms() { return !subRoomIds.isEmpty(); }

    // 멤버 관리
    public boolean addMember(String userId) {
        if (isFull()) return false;
        return memberIds.add(userId);
    }
    public void removeMember(String userId) { memberIds.remove(userId); }
    public boolean isFull() { return memberIds.size() >= maxMembers; }
    public boolean hasMember(String userId) { return memberIds.contains(userId); }
}