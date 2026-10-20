package com.achat.augment.effect;

import com.achat.augment.*;
import com.achat.common.RoomExecution;
import com.achat.message.*;
import com.achat.quest.QuestService;
import com.achat.round.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import static com.achat.augment.AugmentPolicy.*;

@Service
@RequiredArgsConstructor
public class MessageEffectService {
    private final AugmentEffectRegistry registry;
    private final AugmentProgressRepository progress;
    private final AugmentRandom random;
    private final EffectRewards rewards;
    private final MessageRepository messages;
    private final RoundStore rounds;
    private final RoomExecution execution;
    private final MessagePresenter presenter;
    private final SimpMessagingTemplate messaging;
    private final QuestService quests;
    private final AugmentAwardRepository awards;

    /** Executed only for a newly accepted message under the room transaction. */
    public void onMessage(Message message, List<AugmentType> types, boolean scored) {
        message.setRewardEligible(scored);
        if (types.isEmpty() || message.getRoundId() == null) return;
        AugmentProgress state = progress.findById(AugmentProgress.key(message.getRoundId(), message.getSenderId())).orElseThrow();
        MessageEffect effect = registry.createMetadata(new MessageEffectHandler.Input(
                message, state, scored, message.getCreatedAt(), random, rewards));
        if (state.getType() == AugmentType.QUEST_GIVER) {
            var quest = quests.createQuest(message.getRoomId(), message.getId(), message.getSenderId());
            effect = new MessageEffect(AugmentType.QUEST_GIVER, "ACTIVE", null, quest.getExpiresAt(),
                    quest.getRewardCoins(), 0, quest.getType().name(), 0, quest.getTargetCount());
        }
        state.recordMessage();
        progress.save(state);
        message.setAugmentEffects(List.of(effect));
        if (effect.type() == AugmentType.TIME_CAPSULE) message.setNextEffectAt(effect.revealAt());
        if (effect.type() == AugmentType.TIME_BOMB) message.setNextEffectAt(effect.expiresAt());
    }

    public void onReply(Message original, Message reply) {
        if (original == null || !original.getRoomId().equals(reply.getRoomId())
                || original.getSenderId().equals(reply.getSenderId())
                || !Objects.equals(original.getRoundId(), reply.getRoundId()) || !active(original)) return;
        var bomb = original.getAugmentEffects().stream().filter(effect -> effect.type() == AugmentType.TIME_BOMB
                && effect.status().equals("ACTIVE")).findFirst().orElse(null);
        if (bomb == null || reply.getCreatedAt().isBefore(original.getCreatedAt())) return;
        if (!reply.getCreatedAt().isBefore(bomb.expiresAt())) {
            resolve(original, bomb.resolved("EXPLODED", 0));
            return;
        }
        var state = progress.findById(AugmentProgress.key(original.getRoundId(), original.getSenderId())).orElseThrow();
        int points = original.isRewardEligible() ? rewards.grant(original, state, "bomb", BOMB_POINTS) : 0;
        progress.save(state);
        resolve(original, bomb.resolved("SUCCESS", points));
    }

    public void onLike(Message message) {
        if (!active(message) || message.getLikeCount() < LIKE_THRESHOLD) return;
        var effect = message.getAugmentEffects().stream().filter(value -> value.type() == AugmentType.LIKE_OVERDRIVE
                && value.status().equals("ACTIVE")).findFirst().orElse(null);
        if (effect == null) return;
        var state = progress.findById(AugmentProgress.key(message.getRoundId(), message.getSenderId())).orElseThrow();
        int points = state.getRewardCount() < LIKE_MAX_REWARDS && message.isRewardEligible()
                ? rewards.grant(message, state, "overdrive", LIKE_POINTS) : 0;
        progress.save(state);
        resolve(message, effect.resolved("REWARDED", points));
    }

    private boolean active(Message message) {
        Round round = rounds.findByRoomId(message.getRoomId()).orElse(null);
        return round != null && round.getPhase() == RoundPhase.PLAYING
                && Objects.equals(round.getRoundId(), message.getRoundId())
                && Instant.now().isBefore(round.getPhaseEndsAt());
    }

    private void resolve(Message message, MessageEffect effect) {
        message.setAugmentEffects(List.of(effect));
        message.setNextEffectAt(null);
        messages.saveAndFlush(message);
        publish(message);
    }

    public void publish(Message message) {
        var response = presenter.present(message);
        RoomExecution.afterCommit(() -> messaging.convertAndSend(
                "/topic/rooms/" + message.getRoomId() + "/message-effects", response));
        rounds.findByRoomId(message.getRoomId()).ifPresent(round -> {
            rounds.checkpoint(message.getRoomId());
            awards.totals(round.getGameId()).forEach(row -> {
                var score = round.getScores().get((String) row[0]);
                if (score != null) { score.setEffectPoints(((Number) row[1]).intValue()); score.calculate(); }
            });
            var state = com.achat.round.dto.RoundResponse.from(round);
            RoomExecution.afterCommit(() -> messaging.convertAndSend("/topic/rooms/" + message.getRoomId() + "/round", state));
        });
    }

    @Scheduled(fixedDelay = 500)
    public void settleDue() {
        // IDs are read outside the lock; entities are reloaded inside a new transaction after acquiring it.
        var due = messages.findTop100ByNextEffectAtLessThanEqualOrderByNextEffectAtAsc(Instant.now());
        for (Message pending : due) execution.run(pending.getRoomId(), () -> {
            messages.findById(pending.getId()).ifPresent(message -> settle(message, Instant.now(), false));
        });
    }

    public void endCycle(String roomId, String roundId) {
        messages.findByRoomIdAndRoundIdAndNextEffectAtIsNotNull(roomId, roundId)
                .forEach(message -> settle(message, Instant.now(), true));
        var highlights = messages.findByRoomIdAndRoundIdOrderByCreatedAtAsc(roomId, roundId).stream()
                .filter(message -> message.hasAugment(AugmentType.INSIGHT_DROP))
                .sorted(Comparator.comparingInt(Message::getLikeCount).reversed().thenComparing(Message::getCreatedAt))
                .limit(3).toList();
        highlights.forEach(message -> message.setHighlightedAt(Instant.now()));
        var responses = highlights.stream().map(presenter::present).toList();
        if (!responses.isEmpty()) RoomExecution.afterCommit(() -> messaging.convertAndSend(
                "/topic/rooms/" + roomId + "/round/highlights", Map.of("highlights", responses, "roundId", roundId)));
    }

    private void settle(Message message, Instant now, boolean ended) {
        if (message.getNextEffectAt() == null) return;
        for (MessageEffect effect : message.getAugmentEffects()) {
            if (effect.type() == AugmentType.TIME_CAPSULE && !now.isBefore(effect.revealAt())) {
                resolve(message, effect.resolved("REVEALED", 0));
            } else if (effect.type() == AugmentType.TIME_BOMB && effect.status().equals("ACTIVE")
                    && (ended || !now.isBefore(effect.expiresAt()))) {
                resolve(message, effect.resolved(ended && now.isBefore(effect.expiresAt()) ? "CANCELLED" : "EXPLODED", 0));
            }
        }
    }

    public List<com.achat.message.dto.MessageResponse> highlights(String roomId, String gameId) {
        return messages.findByRoomIdAndGameIdOrderByCreatedAtAsc(roomId, gameId).stream()
                .filter(message -> message.hasAugment(AugmentType.INSIGHT_DROP))
                .sorted(Comparator.comparingInt(Message::getLikeCount).reversed()
                        .thenComparing(Message::getCreatedAt))
                .limit(3).map(presenter::present).toList();
    }
}
