package com.achat.security;
import com.achat.room.RoomStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component
@RequiredArgsConstructor
public class RoomAccess {
    private final RoomStore rooms;
    public void requireMember(String roomId, String userId) {
        if (!rooms.findById(roomId).map(room -> room.hasMember(userId)).orElse(false))
            throw new AccessDeniedException("방 참여자만 접근할 수 있습니다");
    }
}
