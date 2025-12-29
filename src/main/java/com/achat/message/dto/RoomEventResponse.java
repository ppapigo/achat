package com.achat.message.dto;

public record RoomEventResponse(
        String type,  // "join" or "leave"
        String userId,
        String roomId
) {}