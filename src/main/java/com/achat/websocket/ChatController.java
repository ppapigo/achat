package com.achat.websocket;

import lombok.RequiredArgsConstructor;

import com.achat.message.MessageCooldownStore;
import com.achat.message.dto.*;
import com.achat.room.Room;
import com.achat.room.RoomStore;
import com.achat.round.RoundService;
import com.achat.sanction.SanctionService;
import com.achat.user.UserStore;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final com.achat.common.RoomExecution execution;
    private final com.achat.message.ChatMessageService chatMessages;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomStore roomStore;
    private final MessageCooldownStore cooldownStore;
    private final RoundService roundService;
    private final UserStore userStore;
    private final SanctionService sanctionService;  // ✅ 추가

    @MessageMapping("/rooms/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, Principal principal) {
        if (principal == null) return;

        execution.run(roomId, () -> joinLocked(roomId, principal.getName()));
    }

    private void joinLocked(String roomId, String userId) {

        // ✅ 정지된 유저 접근 차단
        if (sanctionService.isBanned(userId)) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/error",
                    Map.of(
                            "type", "BANNED",
                            "message", "계정이 정지되어 채팅방에 입장할 수 없습니다."
                    ));
            return;
        }

        Room room = roomStore.findById(roomId).orElse(null);
        if (room == null) return;

        if (!room.hasMember(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("방 참여자만 접근할 수 있습니다");
        }

        roundService.onUserJoin(roomId, userId);

        String username = userStore.findById(userId)
                .map(u -> u.getUsername())
                .orElse("알 수 없음");

        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events",
                Map.of(
                        "type", "join",
                        "userId", userId,
                        "username", username,
                        "roomId", roomId
                ));
    }

    @MessageMapping("/rooms/{roomId}/leave")
    public void leaveRoom(@DestinationVariable String roomId, Principal principal) {
        if (principal == null) return;

        execution.run(roomId, () -> leaveLocked(roomId, principal.getName()));
    }

    private void leaveLocked(String roomId, String userId) {
        Room room = roomStore.findById(roomId).orElse(null);
        if (room == null) return;

        room.removeMember(userId);
        cooldownStore.clearUser(roomId, userId);

        String username = userStore.findById(userId)
                .map(u -> u.getUsername())
                .orElse("알 수 없음");

        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/events",
                Map.of(
                        "type", "leave",
                        "userId", userId,
                        "username", username,
                        "roomId", roomId
                ));

        if (room.getMemberIds().isEmpty()) {
            roomStore.delete(roomId);
            System.out.println("방 삭제됨 (멤버 0명): " + roomId);
        } else if (room.getOwnerId().equals(userId)) {
            String newOwner = room.getMemberIds().iterator().next();
            room.setOwnerId(newOwner);
            System.out.println("방장 위임: " + newOwner);
        }
    }

    @MessageMapping("/rooms/{roomId}/chat")
    public void sendMessage(@DestinationVariable String roomId, @Payload SendMessageRequest request, Principal principal) {
        if (principal != null) chatMessages.send(roomId, principal.getName(), request);
    }

    @MessageMapping("/rooms/{roomId}/like")
    public void toggleLike(@DestinationVariable String roomId, @Payload LikeRequest request, Principal principal) {
        if (principal != null) chatMessages.toggleLike(roomId, principal.getName(), request);
    }

    @org.springframework.messaging.handler.annotation.MessageExceptionHandler({IllegalArgumentException.class, IllegalStateException.class,
            org.springframework.security.access.AccessDeniedException.class, com.achat.security.RateLimitService.RateLimitedException.class})
    public void handleError(Exception error, Principal principal) {
        if (principal != null) messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/error",
                Map.of("type", "CHAT_REJECTED", "message", error.getMessage()));
    }
}
