package com.achat;

import com.achat.augment.*;
import com.achat.augment.effect.*;
import com.achat.message.*;
import com.achat.message.dto.*;
import com.achat.quest.*;
import com.achat.round.RoundPhase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AugmentEffectsTests extends AugmentTestSupport {
    @Autowired MessageEffectService effects;
    @Autowired MessageHistoryController history;
    @Autowired QuestStore quests;
    @Autowired QuestService questService;
    @MockitoSpyBean AugmentRandom random;
    @MockitoSpyBean SimpMessagingTemplate messaging;

    @ParameterizedTest
    @EnumSource(value = AugmentType.class, names = {"SEALED_MESSAGE", "TIME_CAPSULE", "TIME_BOMB",
            "JACKPOT_MESSENGER", "FORBIDDEN_WORD", "GHOST_MESSAGE", "ZERO_GRAVITY", "DOPPELGANGER",
            "LIKE_OVERDRIVE", "FATE_DICE", "MAIN_CHARACTER", "ICE_SEAL"})
    void eachNewEffectIsTargetedPersistedBroadcastAndEndsSafely(AugmentType type) {
        playing(type);
        var sent = send(1, "새 증강 검증을 위한 메시지입니다");
        assertThat(sent.augmentEffects()).hasSize(1);
        assertThat(sent.augmentEffects().getFirst().type()).isEqualTo(type);
        assertThat(send(0, "증강을 보낸 사람의 일반 메시지").augmentEffects()).isEmpty();
        assertThat(send(2, "다른 참가자의 일반 메시지입니다").augmentEffects()).isEmpty();
        assertThat(messages.findById(sent.id()).orElseThrow().getAugmentEffects()).isEqualTo(sent.augmentEffects());
        var fromHistory = history.recentMessages(roomId, ids.get(1), 100).getBody().data().stream()
                .filter(message -> message.id().equals(sent.id())).findFirst().orElseThrow();
        assertThat(fromHistory.augmentEffects()).isEqualTo(sent.augmentEffects());
        verify(messaging).convertAndSend(eq("/topic/rooms/" + roomId + "/messages"), eq(sent));
        assertThat(messages.findByRoomId(roomId)).hasSize(3);
        rounds.advancePhase(roomId);
        assertThat(round().getPhase()).isEqualTo(RoundPhase.PICKING);
        assertThat(states.inRoom(roomId)).isEmpty();
        if (type == AugmentType.TIME_BOMB) assertThat(messages.findById(sent.id()).orElseThrow().getAugmentEffects().getFirst().status()).isEqualTo("CANCELLED");
    }

    @ParameterizedTest
    @EnumSource(value = AugmentType.class, names = {"REVERSE_TEXT", "CAPS_LOCK", "WHISPER", "TURBO_CASTER",
            "ECHO_BURST", "VISUAL_SPELL", "THREAD_WEAVER", "SHADOW_SPEAKER", "SPOTLIGHT", "LINK_BRIDGE",
            "HARMONY_AURA", "CHAOS_ROULETTE", "QUEST_GIVER", "TIME_SHIFTER", "INSIGHT_DROP"})
    void allLegacyAugmentsHaveRealMetadataAndKeepChatWorking(AugmentType type) {
        playing(type);
        String content = "Hello there\nSecond line";
        var sent = send(1, content);
        assertThat(sent.augmentEffects().getFirst().type()).isEqualTo(type);
        if (type == AugmentType.CAPS_LOCK) assertThat(sent.appliedContent()).isEqualTo(content.toUpperCase(Locale.ROOT));
        if (type == AugmentType.WHISPER) assertThat(sent.appliedContent()).isEqualTo(content.toLowerCase(Locale.ROOT));
        if (type == AugmentType.REVERSE_TEXT) assertThat(sent.appliedContent()).isEqualTo(new StringBuilder(content).reverse().toString());
        if (type == AugmentType.ECHO_BURST) assertThat(sent.appliedContent()).isEqualTo(content);
        if (type == AugmentType.SHADOW_SPEAKER) {
            assertThat(sent.senderId()).isNull(); assertThat(sent.senderTitle()).isNull();
            assertThat(sent.senderName()).isEqualTo("익명"); assertThat(sent.anonymous()).isTrue();
            var reply = chat.send(roomId, ids.get(2), new SendMessageRequest("익명 메시지에 대한 답장입니다", sent.id()));
            assertThat(reply.replyToPreview()).startsWith("익명: ").doesNotContain(ids.get(1));
        }
        if (type == AugmentType.QUEST_GIVER) {
            var quest = quests.findByMessageId(sent.id()).orElseThrow();
            assertThat(sent.augmentEffects().getFirst().detail()).isEqualTo(quest.getType().name());
            assertThat(sent.augmentEffects().getFirst().target()).isEqualTo(quest.getTargetCount());
        }
    }

    @Test void capsuleDoesNotLeakContentThroughHistoryOrRepliesBeforeServerDeadline() {
        playing(AugmentType.TIME_CAPSULE);
        var sent = send(1, "10초 동안 아직 공개되지 않는 내용");
        assertThat(sent.content()).isNull(); assertThat(sent.appliedContent()).isNull();
        var effect = sent.augmentEffects().getFirst();
        assertThat(effect.revealAt()).isEqualTo(sent.createdAt().plusSeconds(10));
        assertThat(history.message(roomId, sent.id(), ids.get(2)).getBody().data().content()).isNull();
        assertThatThrownBy(() -> chat.send(roomId, ids.get(2), new SendMessageRequest("숨긴 내용에 답장 시도", sent.id())))
                .isInstanceOf(IllegalArgumentException.class);
        execution.run(roomId, () -> {
            var message = messages.findById(sent.id()).orElseThrow();
            var past = Instant.now().minusSeconds(1);
            message.setAugmentEffects(List.of(new MessageEffect(AugmentType.TIME_CAPSULE, "LOCKED", past, null, 0, 0, null, 0, 0)));
            message.setNextEffectAt(past);
        });
        effects.settleDue();
        assertThat(history.message(roomId, sent.id(), ids.get(2)).getBody().data().content()).contains("공개되지");
        verify(messaging, atLeastOnce()).convertAndSend(eq("/topic/rooms/" + roomId + "/message-effects"), any(MessageResponse.class));
    }

    @Test void timelyReplyResolvesBombExactlyOnceAndRepeatedOldOrCrossRoomRepliesCannotPay() {
        playing(AugmentType.TIME_BOMB);
        var bomb = send(1, "제한 시간 안에 답장해주세요");
        var reply = chat.send(roomId, ids.get(2), new SendMessageRequest("제한 시간 안에 보낸 답장입니다", bomb.id(), "reply-once"));
        assertThat(reply).isNotNull();
        var resolved = messages.findById(bomb.id()).orElseThrow().getAugmentEffects().getFirst();
        assertThat(resolved.status()).isEqualTo("SUCCESS"); assertThat(resolved.points()).isEqualTo(10);
        chat.send(roomId, ids.get(2), new SendMessageRequest("제한 시간 안에 보낸 답장입니다", bomb.id(), "reply-once"));
        chat.send(roomId, ids.get(3), new SendMessageRequest("두 번째 사람의 추가 답장", bomb.id()));
        assertThat(awards.findById(bomb.id() + ":bomb").orElseThrow().getPoints()).isEqualTo(10);
        assertThat(awards.totals(round().getGameId()).getFirst()[1]).isEqualTo(10L);
        var foreign = new Message("another-room", ids.get(1), null, "다른 방 메시지", "다른 방 메시지", null);
        messages.save(foreign);
        assertThatThrownBy(() -> chat.send(roomId, ids.get(4), new SendMessageRequest("다른 방 답장 시도", foreign.getId())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chat.toggleLike(roomId, ids.get(4), new LikeRequest(foreign.getId())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void expiredBombIsSettledByServerWithoutAward() {
        playing(AugmentType.TIME_BOMB);
        var bomb = send(1, "시한폭탄 시간이 끝나는 메시지");
        execution.run(roomId, () -> {
            var message = messages.findById(bomb.id()).orElseThrow();
            var past = Instant.now().minusSeconds(1);
            message.setAugmentEffects(List.of(new MessageEffect(AugmentType.TIME_BOMB, "ACTIVE", null, past, 0, 0, null, 0, 0)));
            message.setNextEffectAt(past);
        });
        chat.send(roomId, ids.get(2), new SendMessageRequest("제한 시간이 지난 답장입니다", bomb.id()));
        effects.settleDue();
        assertThat(messages.findById(bomb.id()).orElseThrow().getAugmentEffects().getFirst().status()).isEqualTo("EXPLODED");
        assertThat(awards.findById(bomb.id() + ":bomb")).isEmpty();
    }

    @Test void jackpotAndMessageRetransmissionDoNotDuplicateDrawsOrRewards() {
        playing(AugmentType.JACKPOT_MESSENGER);
        doReturn(0).when(random).nextInt(100);
        var request = new SendMessageRequest("잭팟 추첨 대상이 되는 긴 메시지", null, "same-message");
        var first = chat.send(roomId, ids.get(1), request);
        var repeated = chat.send(roomId, ids.get(1), request);
        assertThat(repeated.id()).isEqualTo(first.id());
        assertThat(repeated.augmentEffects()).isEqualTo(first.augmentEffects());
        assertThat(first.augmentEffects().getFirst().points()).isEqualTo(10);
        cooldowns.clearUser(roomId, ids.get(1));
        var spam = send(1, request.content());
        assertThat(spam.augmentEffects().getFirst().points()).isZero();
        var state = progress.findById(AugmentProgress.key(round().getRoundId(), ids.get(1))).orElseThrow();
        assertThat(state.getDraws()).isEqualTo(1); assertThat(state.getAwardedPoints()).isEqualTo(10);
        assertThat(messages.findByRoomId(roomId)).hasSize(2);
    }

    @Test void fiveLikesPayOnceAndUnlikeRelikeCannotFarmPoints() {
        playing(AugmentType.LIKE_OVERDRIVE);
        var sent = send(1, "공감이 모이는 메시지입니다");
        for (int voter : List.of(0, 2, 3, 4, 5)) chat.toggleLike(roomId, ids.get(voter), new LikeRequest(sent.id()));
        var awarded = messages.findById(sent.id()).orElseThrow();
        assertThat(awarded.getAugmentEffects().getFirst().points()).isEqualTo(10);
        chat.toggleLike(roomId, ids.get(5), new LikeRequest(sent.id()));
        chat.toggleLike(roomId, ids.get(5), new LikeRequest(sent.id()));
        assertThat(awards.totals(round().getGameId()).getFirst()[1]).isEqualTo(10L);
        assertThatThrownBy(() -> chat.toggleLike(roomId, ids.get(1), new LikeRequest(sent.id()))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void mainCharacterIsConsumedByOnlyTheFirstAcceptedMessage() {
        playing(AugmentType.MAIN_CHARACTER);
        assertThat(send(1, "첫 주인공 메시지입니다").augmentEffects().getFirst().status()).isEqualTo("ACTIVE");
        cooldowns.clearUser(roomId, ids.get(1));
        assertThat(send(1, "다음 메시지는 일반 표시입니다").augmentEffects().getFirst().status()).isEqualTo("SPENT");
        assertThat(messages.findByRoomId(roomId)).hasSize(2);
    }

    @Test void forbiddenWordNormalizesCaseAndWhitespaceAndCapsPenaltyAtFiveMessages() {
        playing(AugmentType.FORBIDDEN_WORD);
        var word = progress.findById(AugmentProgress.key(round().getRoundId(), ids.get(1))).orElseThrow().getForbiddenWord();
        for (int i = 0; i < 7; i++) {
            cooldowns.clearUser(roomId, ids.get(1));
            send(1, String.join(" \t", word.toUpperCase(Locale.ROOT).split("")) + " 사용 " + i);
        }
        var state = progress.findById(AugmentProgress.key(round().getRoundId(), ids.get(1))).orElseThrow();
        assertThat(state.getViolations()).isEqualTo(5);
        assertThat(awards.totals(round().getGameId()).getFirst()[1]).isEqualTo(-15L);
    }

    @Test void forbiddenWordSuccessBonusIsAwardedOnlyOnce() {
        playing(AugmentType.FORBIDDEN_WORD);
        for (int i = 0; i < 7; i++) {
            cooldowns.clearUser(roomId, ids.get(1)); send(1, "안전하게 미션을 진행하는 대화 " + i);
        }
        assertThat(awards.totals(round().getGameId()).getFirst()[1]).isEqualTo(15L);
    }

    @Test void insightHighlightAndRoundRewardsAreBroadcastOnlyOnceAtGameEnd() {
        playing(AugmentType.INSIGHT_DROP);
        var sent = send(1, "라운드에서 기억할 만한 이야기");
        execution.run(roomId, () -> org.springframework.test.util.ReflectionTestUtils.setField(round(), "currentPhaseNumber", 3));
        rounds.advancePhase(roomId);
        int coins = users.findById(ids.get(1)).orElseThrow().getCoins();
        rounds.advancePhase(roomId);
        assertThat(users.findById(ids.get(1)).orElseThrow().getCoins()).isEqualTo(coins);
        var event = org.mockito.ArgumentCaptor.forClass(com.achat.round.dto.RoundEndResponse.class);
        verify(messaging).convertAndSend(eq("/topic/rooms/" + roomId + "/round/end"), event.capture());
        assertThat(event.getValue().highlights()).extracting(MessageResponse::id).containsExactly(sent.id());
    }

    @ParameterizedTest
    @EnumSource(QuestType.class)
    void questProgressAndRewardsPersistAndCannotBeRepeated(QuestType type) {
        playing(AugmentType.QUEST_GIVER);
        var sent = send(1, "기존 퀘스트 보상 회귀 테스트");
        execution.run(roomId, () -> {
            var quest = quests.findByMessageId(sent.id()).orElseThrow();
            org.springframework.test.util.ReflectionTestUtils.setField(quest, "type", type);
            org.springframework.test.util.ReflectionTestUtils.setField(quest, "targetCount", type.getDefaultTarget());
            org.springframework.test.util.ReflectionTestUtils.setField(quest, "rewardCoins", type.getRewardCoins());
        });
        int authorCoins = users.findById(ids.get(1)).orElseThrow().getCoins();
        int participantCoins = users.findById(ids.get(0)).orElseThrow().getCoins();
        int[] participants = {0, 2, 3, 4, 5};
        for (int i = 0; i < type.getDefaultTarget(); i++) {
            int participant = participants[i];
            if (type == QuestType.LIKE_QUEST) {
                chat.toggleLike(roomId, ids.get(participant), new LikeRequest(sent.id()));
                chat.toggleLike(roomId, ids.get(participant), new LikeRequest(sent.id()));
                chat.toggleLike(roomId, ids.get(participant), new LikeRequest(sent.id()));
            } else {
                var request = new SendMessageRequest("서로 다른 참여자의 퀘스트 답장", sent.id(), "quest-" + i);
                chat.send(roomId, ids.get(participant), request);
                chat.send(roomId, ids.get(participant), request);
            }
            assertThat(quests.findByMessageId(sent.id()).orElseThrow().getCurrentCount()).isEqualTo(i + 1);
        }
        questService.onReply("another-room", sent.id(), ids.get(6));
        questService.onReply(roomId, sent.id(), ids.get(6));
        questService.onLike(roomId, sent.id(), ids.get(6));
        assertThat(quests.findByMessageId(sent.id()).orElseThrow().isCompleted()).isTrue();
        assertThat(users.findById(ids.get(1)).orElseThrow().getCoins()).isEqualTo(authorCoins + type.getRewardCoins());
        assertThat(users.findById(ids.get(0)).orElseThrow().getCoins()).isEqualTo(participantCoins + type.getRewardCoins() / 2);
        var restored = history.message(roomId, sent.id(), ids.get(2)).getBody().data().augmentEffects().getFirst();
        assertThat(restored.status()).isEqualTo("COMPLETE");
        assertThat(restored.progress()).isEqualTo(type.getDefaultTarget());
    }

    @Test void transactionFailureRestoresScoringAndAllowsMessageRetry() {
        playing(AugmentType.JACKPOT_MESSENGER);
        doReturn(0).when(random).nextInt(100);
        assertThatThrownBy(() -> execution.run(roomId, () -> {
            send(1, "저장 실패 시 점수와 추첨을 되돌리는 메시지");
            throw new IllegalStateException("simulate database failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(messages.findByRoomId(roomId)).isEmpty();
        assertThat(round().getScores().get(ids.get(1)).getMessageCount()).isZero();
        assertThat(progress.findById(AugmentProgress.key(round().getRoundId(), ids.get(1))).orElseThrow().getDraws()).isZero();
        var retry = send(1, "저장 실패 시 점수와 추첨을 되돌리는 메시지");
        assertThat(retry.augmentEffects().getFirst().points()).isEqualTo(10);
        assertThat(round().getScores().get(ids.get(1)).getMessageCount()).isEqualTo(1);
    }

    @Test void failedRoundEndRestoresPhaseSelectionsScoresAndRewards() {
        playing(AugmentType.INSIGHT_DROP);
        send(1, "라운드 보상 트랜잭션을 검증하는 메시지");
        execution.run(roomId, () -> org.springframework.test.util.ReflectionTestUtils.setField(round(), "currentPhaseNumber", 3));
        String cycle = round().getRoundId();
        int coins = users.findById(ids.get(1)).orElseThrow().getCoins();
        assertThatThrownBy(() -> execution.run(roomId, () -> {
            rounds.advancePhase(roomId);
            throw new IllegalStateException("simulate reward commit failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(round().getPhase()).isEqualTo(RoundPhase.PLAYING);
        assertThat(round().getRoundId()).isEqualTo(cycle);
        assertThat(round().getScores().get(ids.get(1)).getMessageCount()).isEqualTo(1);
        assertThat(states.received(roomId, cycle, ids.get(1))).isPresent();
        assertThat(users.findById(ids.get(1)).orElseThrow().getCoins()).isEqualTo(coins);
        verify(messaging, never()).convertAndSend(eq("/topic/rooms/" + roomId + "/round/end"), any(Object.class));
        rounds.advancePhase(roomId);
        assertThat(round().getPhase()).isEqualTo(RoundPhase.WAITING);
        assertThat(users.findById(ids.get(1)).orElseThrow().getCoins()).isGreaterThan(coins);
    }

    @Test void concurrentRetransmissionsDrawAndAwardOnlyOnce() throws Exception {
        playing(AugmentType.JACKPOT_MESSENGER);
        doReturn(0).when(random).nextInt(100);
        var request = new SendMessageRequest("동시에 도착하는 동일 메시지 전송", null, "concurrent-retry");
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var tasks = java.util.stream.IntStream.range(0, 2).mapToObj(index -> executor.submit(() -> {
                barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                return chat.send(roomId, ids.get(1), request);
            })).toList();
            var first = tasks.getFirst().get(10, java.util.concurrent.TimeUnit.SECONDS);
            var second = tasks.getLast().get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(first.id()).isEqualTo(second.id());
        }
        assertThat(messages.findByRoomId(roomId)).hasSize(1);
        assertThat(awards.totals(round().getGameId()).getFirst()[1]).isEqualTo(10L);
        assertThat(progress.findById(AugmentProgress.key(round().getRoundId(), ids.get(1))).orElseThrow().getDraws()).isEqualTo(1);
    }

    @Test void concurrentFifthAndSixthLikesPayOnlyOnce() throws Exception {
        playing(AugmentType.LIKE_OVERDRIVE);
        var sent = send(1, "동시에 달리는 공감을 검증합니다");
        for (int voter : List.of(0, 2, 3, 4)) chat.toggleLike(roomId, ids.get(voter), new LikeRequest(sent.id()));
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var tasks = List.of(5, 6).stream().map(voter -> executor.submit(() -> {
                barrier.await(5, java.util.concurrent.TimeUnit.SECONDS);
                return chat.toggleLike(roomId, ids.get(voter), new LikeRequest(sent.id()));
            })).toList();
            for (var task : tasks) task.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(messages.findById(sent.id()).orElseThrow().getLikeCount()).isEqualTo(6);
        assertThat(awards.totals(round().getGameId()).getFirst()[1]).isEqualTo(10L);
        assertThat(progress.findById(AugmentProgress.key(round().getRoundId(), ids.get(1))).orElseThrow().getRewardCount()).isEqualTo(1);
    }
}
