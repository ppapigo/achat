package com.achat.message;

import com.achat.common.ApiResponse;
import com.achat.message.dto.MessageResponse;
import com.achat.room.RoomStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Transactional(readOnly = true)
@RequestMapping("/rooms/{roomId}/messages")
public class MessageHistoryController {
    private final RoomStore rooms;
    private final MessageStore messages;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageResponse>>> recentMessages(
            @PathVariable String roomId,
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "100") int limit) {
        var room = rooms.findById(roomId);
        if (room.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("방을 찾을 수 없습니다"));
        if (!room.get().hasMember(userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("채팅방 멤버만 메시지를 조회할 수 있습니다"));
        return ResponseEntity.ok(ApiResponse.ok(messages.findRecentByRoomId(roomId, limit)
                .stream().map(MessageResponse::from).toList()));
    }
}
