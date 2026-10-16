package com.achat.websocket;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.augment.AugmentService;
import com.achat.augment.AugmentType;
import com.achat.message.Message;
import com.achat.message.MessageCooldownStore;
import com.achat.message.MessageStore;
import com.achat.message.UserMessageTracker;
import com.achat.message.dto.*;
import com.achat.quest.QuestService;
import com.achat.room.Room;
import com.achat.room.RoomStore;
import com.achat.round.RoundService;
import com.achat.sanction.SanctionService;
import com.achat.shop.EquipSlot;
import com.achat.shop.ShopItem;
import com.achat.shop.ShopService;
import com.achat.shop.UserEquipment;
import com.achat.user.UserStore;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Transactional
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomStore roomStore;
    private final MessageStore messageStore;
    private final AugmentService augmentService;
    private final MessageCooldownStore cooldownStore;
    private final RoundService roundService;
    private final ShopService shopService;
    private final UserStore userStore;
    private final QuestService questService;
    private final SanctionService sanctionService;  // ✅ 추가

    @MessageMapping("/rooms/{roomId}/join")
    public void joinRoom(@DestinationVariable String roomId, Principal principal) {
        if (principal == null) return;

        String userId = principal.getName();

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
            room.addMember(userId);
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

        String userId = principal.getName();
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
    public void sendMessage(@DestinationVariable String roomId,
                            @Payload SendMessageRequest request,
                            Principal principal) {
        if (principal == null) return;

        String userId = principal.getName();

        // ✅ 정지된 유저 메시지 차단
        if (sanctionService.isBanned(userId)) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/error",
                    Map.of(
                            "type", "BANNED",
                            "message", "계정이 정지되어 메시지를 보낼 수 없습니다."
                    ));
            return;
        }

        // ✅ 채금된 유저 메시지 차단
        if (!sanctionService.canChat(userId)) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/error",
                    Map.of(
                            "type", "MUTED",
                            "message", "채팅이 금지되어 메시지를 보낼 수 없습니다."
                    ));
            return;
        }

        Room room = roomStore.findById(roomId).orElse(null);
        if (room == null || !room.hasMember(userId)) return;

        String content = request.content();
        if (content == null || content.isBlank()) return;

        // 다른 참여자가 고른 증강을 발신자에게 적용
        List<AugmentType> augmentTypes = augmentService.getAppliedAugmentTypes(roomId, userId);

        // 증강별 쿨타임 배율 계산
        double cooldownMultiplier = getCooldownMultiplier(augmentTypes);

        // 쿨타임 체크 (배율 적용!)
        UserMessageTracker tracker = cooldownStore.getTracker(roomId, userId);
        long remainingMs = tracker.checkCooldownWithMultiplier(content.length(), cooldownMultiplier);

        if (remainingMs > 0) {
            CooldownNotice notice = CooldownNotice.of(remainingMs, tracker.shouldWarn());
            messagingTemplate.convertAndSendToUser(userId, "/queue/cooldown", notice);
            return;
        }

        tracker.recordMessage(content.length());

        // 증강 적용
        String appliedContent = augmentService.applyAugments(roomId, userId, content, augmentTypes);

        // 발신자 칭호 조회
        String senderTitle = getSenderTitle(userId);

        // 메시지 생성
        String augmentType = augmentTypes.isEmpty() ? null : augmentTypes.stream()
                .map(Enum::name).collect(Collectors.joining(","));
        Message message = new Message(roomId, userId, senderTitle, content, appliedContent, augmentType, request.replyToId());

        // 답글 미리보기 설정
        if (request.replyToId() != null) {
            messageStore.findById(request.replyToId()).ifPresent(replyTo -> {
                String preview = replyTo.getContent();
                if (preview.length() > 30) {
                    preview = preview.substring(0, 30) + "...";
                }
                message.setReplyToPreview(preview);
            });
        }

        messageStore.save(message);

        // 퀘스트 생성 (QUEST_GIVER 증강일 때)
        if (augmentTypes.contains(AugmentType.QUEST_GIVER)) {
            questService.createQuest(roomId, message.getId(), userId);
        }

        // 답장이면 퀘스트 진행 체크
        if (request.replyToId() != null) {
            questService.onReply(roomId, request.replyToId(), userId);
        }

        roundService.onMessage(roomId, userId, content);

        MessageResponse response = MessageResponse.from(message);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", response);
    }

    /**
     * 증강별 쿨타임 배율 반환
     */
    private double getCooldownMultiplier(List<AugmentType> augmentTypes) {
        // 여러 속도 증강을 받으면 가장 큰 감소 효과를 적용
        if (augmentTypes.contains(AugmentType.TURBO_CASTER)) return 0.6;
        if (augmentTypes.contains(AugmentType.TIME_SHIFTER)) return 0.8;
        return 1.0;
    }

    /**
     * 발신자 칭호 조회
     */
    private String getSenderTitle(String userId) {
        UserEquipment equipment = shopService.getEquipment(userId);
        String titleItemId = equipment.getEquipped(EquipSlot.TITLE);

        if (titleItemId == null) return null;

        ShopItem item = shopService.getItem(titleItemId);
        return item != null ? item.getName() : null;
    }

    @MessageMapping("/rooms/{roomId}/like")
    public void toggleLike(@DestinationVariable String roomId,
                           @Payload LikeRequest request,
                           Principal principal) {
        if (principal == null) return;

        String userId = principal.getName();
        Room room = roomStore.findById(roomId).orElse(null);
        if (room == null || !room.hasMember(userId)) return;

        messageStore.findById(request.messageId()).ifPresent(message -> {
            boolean liked = message.toggleLike(userId);

            LikeResponse response = new LikeResponse(
                    message.getId(),
                    message.getLikeCount(),
                    liked,
                    userId
            );

            messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/likes", response);
        });
    }
}
