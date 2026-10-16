package com.achat.room;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import com.achat.persistence.JpaEntity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "rooms", indexes = @Index(columnList = "owner_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Room extends JpaEntity {

    @Id
    @Column(length = 36)
    private String id;
    private String name;
    @Column(length = 36)
    @Setter
    private String ownerId;
    private int maxMembers;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_members", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "user_id", length = 36)
    private Set<String> memberIds;
    private Instant createdAt;
    @Enumerated(EnumType.STRING)
    @Setter
    private RoomType type = RoomType.PERMANENT;

    // 시즌 연결 (영구방용)
    @Setter
    private int seasonId = 0;

    // 시즌별 통계 (시즌 종료 시 리셋)
    private int seasonMessageCount = 0;
    @Setter
    private int seasonActiveMembers = 0;

    // 계층 구조
    @Setter
    private String parentId = null;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_sub_rooms", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "sub_room_id", length = 36)
    private Set<String> subRoomIds = new HashSet<>();

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

    // 시즌 관련
    public void addSeasonMessage() { this.seasonMessageCount++; }

    public void resetSeasonStats() {
        this.seasonMessageCount = 0;
        this.seasonActiveMembers = 0;
    }

    // 타입 체크
    public boolean isMatchRoom() { return type == RoomType.MATCH; }
    public boolean isPartyRoom() { return type == RoomType.PARTY; }
    public boolean isPermanentRoom() { return type == RoomType.PERMANENT; }

    // 계층 구조
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
