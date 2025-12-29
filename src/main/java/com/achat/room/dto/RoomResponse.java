package com.achat.room.dto;

import com.achat.room.Room;

import java.util.Set;

public record RoomResponse(
        String id,
        String name,
        String ownerId,
        int maxMembers,
        int currentMembers,
        boolean matchRoom,
        String parentId,
        Set<String> subRoomIds,
        boolean isRootRoom
) {
    public static RoomResponse from(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getOwnerId(),
                room.getMaxMembers(),
                room.getMemberIds().size(),
                room.isMatchRoom(),
                room.getParentId(),
                room.getSubRoomIds(),
                room.isRootRoom()
        );
    }
}