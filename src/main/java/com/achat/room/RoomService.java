package com.achat.room;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.room.dto.CreateRoomRequest;
import com.achat.room.dto.RoomResponse;
import com.achat.roomrank.RoomRankService;  // 추가!
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class RoomService {

    private final com.achat.common.RoomExecution execution;
    private final RoomStore roomStore;
    private final RoomRankService roomRankService;  // 추가!
    private final com.achat.security.AccountAccess access;

    public RoomResponse createRoom(CreateRoomRequest request, String ownerId) {
        access.active(ownerId);
        validateName(request.name());
        if (request.maxMembers() < 2 || request.maxMembers() > 12) throw new IllegalArgumentException("방 정원은 2~12명이어야 합니다");
        Room room = new Room(request.name(), ownerId, request.maxMembers());
        roomStore.save(room);

        // 영구방이면 랭킹에 등록 (추가!)
        if (room.isPermanentRoom()) {
            roomRankService.registerRoom(room.getId());
        }

        return RoomResponse.from(room);
    }

    public List<RoomResponse> listRooms() {
        return roomStore.findAll().stream()
                .filter(Room::isRootRoom)  // 루트 방만
                .filter(Room::isPermanentRoom)
                .map(RoomResponse::from)
                .toList();
    }
    public RoomResponse getRoom(String roomId, String userId) {
        access.active(userId);
        Room room = roomStore.findById(roomId).orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));
        if (!room.isPermanentRoom() && !room.hasMember(userId))
            throw new org.springframework.security.access.AccessDeniedException("방 접근 권한이 없습니다");
        return RoomResponse.from(room);
    }

    public RoomResponse joinRoom(String roomId, String userId) {
        return execution.run(roomId, () -> joinRoomLocked(roomId, userId));
    }
    private RoomResponse joinRoomLocked(String roomId, String userId) {
        access.active(userId);
        Room room = roomStore.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));

        if (!room.isPermanentRoom() && !room.hasMember(userId))
            throw new org.springframework.security.access.AccessDeniedException("초대 또는 매칭된 참여자만 입장할 수 있습니다");
        if (room.getParentId() != null && !roomStore.findById(room.getParentId()).map(parent -> parent.hasMember(userId)).orElse(false))
            throw new org.springframework.security.access.AccessDeniedException("상위 방 참여자만 입장할 수 있습니다");
        if (room.isFull() && !room.hasMember(userId)) {
            throw new IllegalStateException("방이 가득 찼습니다");
        }

        room.addMember(userId);
        return RoomResponse.from(room);
    }

    public void leaveRoom(String roomId, String userId) {
        execution.run(roomId, () -> leaveRoomLocked(roomId, userId));
    }
    private void leaveRoomLocked(String roomId, String userId) {
        Room room = roomStore.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));

        if (!room.hasMember(userId)) throw new org.springframework.security.access.AccessDeniedException("방 참여자가 아닙니다");
        room.removeMember(userId);

        // 방장이 나가면
        if (room.getOwnerId().equals(userId)) {
            if (room.getMemberIds().isEmpty()) {
                // 아무도 없으면 방 삭제
                roomStore.remove(roomId);
            } else {
                // 다른 멤버에게 방장 양도
                String newOwner = room.getMemberIds().iterator().next();
                room.setOwnerId(newOwner);
            }
        }
    }
    public RoomResponse createSubRoom(String parentId, String name, String userId) {
        return execution.run(parentId, () -> createSubRoomLocked(parentId, name, userId));
    }
    private RoomResponse createSubRoomLocked(String parentId, String name, String userId) {
        access.active(userId);
        validateName(name);
        Room parent = roomStore.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("상위 방을 찾을 수 없습니다"));

        // 방장만 하위 방 생성 가능
        if (!parent.getOwnerId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("방장만 하위 방을 만들 수 있습니다");
        }

        // 하위 방 생성
        Room subRoom = new Room(name, userId, parent.getMaxMembers());
        subRoom.setParentId(parentId);
        subRoom.setType(parent.getType());

        // 상위 방 멤버 자동 추가
        parent.getMemberIds().forEach(subRoom::addMember);

        roomStore.save(subRoom);
        parent.addSubRoom(subRoom.getId());

        return RoomResponse.from(subRoom);
    }

    public List<RoomResponse> getSubRooms(String parentId) {
        Room parent = roomStore.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));

        return parent.getSubRoomIds().stream()
                .map(roomStore::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(RoomResponse::from)
                .toList();
    }

    public void deleteSubRoom(String parentId, String subRoomId, String userId) {
        execution.run(parentId, () -> deleteSubRoomLocked(parentId, subRoomId, userId));
    }
    private void deleteSubRoomLocked(String parentId, String subRoomId, String userId) {
        access.active(userId);
        Room parent = roomStore.findById(parentId)
                .orElseThrow(() -> new IllegalArgumentException("상위 방을 찾을 수 없습니다"));

        if (!parent.getOwnerId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("방장만 하위 방을 삭제할 수 있습니다");
        }
        Room child = roomStore.findById(subRoomId).orElseThrow(() -> new IllegalArgumentException("하위 방을 찾을 수 없습니다"));
        if (!parentId.equals(child.getParentId()) || !parent.getSubRoomIds().contains(subRoomId))
            throw new org.springframework.security.access.AccessDeniedException("해당 방의 하위 방이 아닙니다");
        parent.removeSubRoom(subRoomId);
        roomStore.remove(subRoomId);
    }
    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) throw new IllegalArgumentException("방 이름은 1~100자여야 합니다");
    }
}
