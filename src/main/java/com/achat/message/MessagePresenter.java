package com.achat.message;

import com.achat.augment.AugmentType;
import com.achat.message.dto.MessageResponse;
import com.achat.user.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MessagePresenter {
    private final MessageStore messages;
    private final UserStore users;
    private final com.achat.quest.QuestStore quests;

    public MessageResponse present(Message message) {
        String name = message.hasAugment(AugmentType.SHADOW_SPEAKER) ? "익명"
                : users.findById(message.getSenderId()).map(user -> user.getUsername()).orElse("참여자");
        String preview = null;
        if (message.getReplyToId() != null) {
            preview = messages.findById(message.getReplyToId())
                    .filter(parent -> parent.getRoomId().equals(message.getRoomId()))
                    .map(parent -> {
                        var safe = MessageResponse.from(parent);
                        String content = safe.appliedContent() == null ? safe.content() : safe.appliedContent();
                        if (content == null) return "아직 공개되지 않은 메시지";
                        return (safe.anonymous() ? "익명: " : "") + content.substring(0, Math.min(content.length(), 60));
                    }).orElse("이전 메시지");
        }
        var response = MessageResponse.from(message, name, preview);
        if (message.hasAugment(AugmentType.QUEST_GIVER)) {
            var quest = quests.findByMessageId(message.getId()).orElse(null);
            if (quest != null) response = response.withEffects(List.of(new com.achat.augment.effect.MessageEffect(
                    AugmentType.QUEST_GIVER, quest.isCompleted() ? "COMPLETE" : quest.isCancelled() || quest.isExpired() ? "CANCELLED" : "ACTIVE",
                    null, quest.getExpiresAt(), quest.getRewardCoins(), 0, quest.getType().name(), quest.getCurrentCount(), quest.getTargetCount())));
        }
        return response;
    }
}
