package com.achat.message;

import com.achat.augment.*;
import com.achat.augment.effect.MessageEffectService;
import com.achat.common.RoomExecution;
import com.achat.message.dto.*;
import com.achat.quest.QuestService;
import com.achat.room.RoomStore;
import com.achat.round.*;
import com.achat.sanction.SanctionService;
import com.achat.shop.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final RoomExecution execution;
    private final RoomStore rooms;
    private final MessageRepository messages;
    private final MessageCooldownStore cooldowns;
    private final AugmentService augments;
    private final MessageEffectService effects;
    private final MessagePresenter presenter;
    private final RoundService rounds;
    private final RoundStore roundStore;
    private final QuestService quests;
    private final ShopService shop;
    private final SanctionService sanctions;
    private final SimpMessagingTemplate messaging;

    public MessageResponse send(String roomId, String userId, SendMessageRequest request) {
        return execution.run(roomId, () -> {
            requireMember(roomId, userId);
            if (sanctions.isBanned(userId)) throw new IllegalArgumentException("계정이 정지되어 메시지를 보낼 수 없습니다");
            if (!sanctions.canChat(userId)) throw new IllegalArgumentException("채팅이 금지되어 메시지를 보낼 수 없습니다");
            String content = request.content();
            if (content == null || content.isBlank() || content.length() > 2000) {
                throw new IllegalArgumentException("메시지는 1~2000자로 입력해주세요");
            }
            if (request.clientMessageId() != null) {
                if (!request.clientMessageId().matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("잘못된 메시지 요청 ID입니다");
                var existing = messages.findByRoomIdAndSenderIdAndClientMessageId(roomId, userId, request.clientMessageId());
                if (existing.isPresent()) {
                    var response = presenter.present(existing.get());
                    publish(roomId, response);
                    return response;
                }
            }
            Message parent = request.replyToId() == null ? null : messages.findById(request.replyToId())
                    .filter(message -> roomId.equals(message.getRoomId()))
                    .orElseThrow(() -> new IllegalArgumentException("같은 방의 메시지에만 답장할 수 있습니다"));
            if (parent != null && MessageResponse.from(parent).content() == null) {
                throw new IllegalArgumentException("공개된 메시지에 답장해주세요");
            }
            var types = augments.getAppliedAugmentTypes(roomId, userId);
            var tracker = cooldowns.getTracker(roomId, userId);
            double multiplier = types.contains(AugmentType.TURBO_CASTER) ? 0.6 : 1.0;
            long remaining = tracker.checkCooldownWithMultiplier(content.length(), multiplier);
            if (remaining > 0) {
                var notice = CooldownNotice.of(remaining, tracker.shouldWarn());
                RoomExecution.afterCommit(() -> messaging.convertAndSendToUser(userId, "/queue/cooldown", notice));
                return null;
            }
            Round round = roundStore.findByRoomId(roomId).orElse(null);
            boolean playing = round != null && round.getPhase() == RoundPhase.PLAYING
                    && Instant.now().isBefore(round.getPhaseEndsAt());
            if (!playing) types = List.of();
            Message message = new Message(roomId, userId, senderTitle(userId), content,
                    augments.applyAugments(roomId, userId, content, types),
                    types.isEmpty() ? null : types.getFirst().name(), request.replyToId());
            message.setClientMessageId(request.clientMessageId());
            if (playing) {
                message.setRoundId(round.getRoundId());
                message.setGameId(round.getGameId());
            }
            boolean scored = rounds.onMessage(roomId, userId, content);
            effects.onMessage(message, types, scored);
            messages.saveAndFlush(message);
            if (parent != null) {
                effects.onReply(parent, message);
                if (message.getRoundId() != null && Objects.equals(parent.getRoundId(), message.getRoundId())) {
                    quests.onReply(roomId, parent.getId(), userId);
                    effects.publish(parent);
                }
            }
            RoomExecution.afterCommit(() -> tracker.recordMessage(content.length()));
            var response = presenter.present(message);
            publish(roomId, response);
            var state = rounds.getRoundState(roomId);
            RoomExecution.afterCommit(() -> messaging.convertAndSend("/topic/rooms/" + roomId + "/round", state));
            return response;
        });
    }

    public MessageResponse toggleLike(String roomId, String userId, LikeRequest request) {
        return execution.run(roomId, () -> {
            requireMember(roomId, userId);
            if (sanctions.isBanned(userId)) throw new IllegalArgumentException("정지된 계정입니다");
            Message message = messages.findById(request.messageId())
                    .filter(value -> roomId.equals(value.getRoomId()))
                    .orElseThrow(() -> new IllegalArgumentException("같은 방의 메시지만 공감할 수 있습니다"));
            if (userId.equals(message.getSenderId())) throw new IllegalArgumentException("자신의 메시지는 공감할 수 없습니다");
            if (MessageResponse.from(message).content() == null) throw new IllegalArgumentException("공개된 메시지를 공감해주세요");
            boolean liked = message.toggleLike(userId);
            if (liked) {
                effects.onLike(message);
                Round round = roundStore.findByRoomId(roomId).orElse(null);
                if (round != null && round.getPhase() == RoundPhase.PLAYING
                        && Objects.equals(round.getRoundId(), message.getRoundId()) && Instant.now().isBefore(round.getPhaseEndsAt())) {
                    quests.onLike(roomId, message.getId(), userId);
                }
            }
            messages.saveAndFlush(message);
            var legacy = new LikeResponse(message.getId(), message.getLikeCount(), liked, userId, message.getRevision());
            RoomExecution.afterCommit(() -> messaging.convertAndSend("/topic/rooms/" + roomId + "/likes", legacy));
            effects.publish(message);
            return presenter.present(message);
        });
    }

    private void requireMember(String roomId, String userId) {
        var room = rooms.findById(roomId).orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));
        if (!room.hasMember(userId)) throw new IllegalArgumentException("같은 방의 참여자만 사용할 수 있습니다");
    }
    private String senderTitle(String userId) {
        String title = shop.getEquipment(userId).getEquipped(EquipSlot.TITLE);
        var item = title == null ? null : shop.getItem(title);
        return item == null ? null : item.getName();
    }
    private void publish(String roomId, MessageResponse response) {
        RoomExecution.afterCommit(() -> messaging.convertAndSend("/topic/rooms/" + roomId + "/messages", response));
    }
}
