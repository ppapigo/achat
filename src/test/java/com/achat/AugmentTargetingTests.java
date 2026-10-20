package com.achat;

import com.achat.augment.*;
import com.achat.augment.dto.PickAugmentRequest;
import com.achat.round.RoundPhase;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class AugmentTargetingTests extends AugmentTestSupport {
    private PickAugmentRequest request(int target) {
        return new PickAugmentRequest(AugmentType.CAPS_LOCK, ids.get(target), round().getRoundId());
    }
    @Test void selfMissingAndOtherRoomTargetsDoNotConsumeThePick() {
        var state = offer(0, AugmentType.CAPS_LOCK);
        for (String target : Arrays.asList(ids.get(0), "outsider", "", null)) {
            assertThatThrownBy(() -> augments.pick(roomId, ids.get(0),
                    new PickAugmentRequest(AugmentType.CAPS_LOCK, target, round().getRoundId())))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(state.hasSelected()).isFalse();
        }
        assertThatThrownBy(() -> augments.getOptions(roomId, "outsider")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void duplicateRecipientsReturnConflictAndTheLosingSenderCanRetry() {
        pick(0, 2, AugmentType.CAPS_LOCK);
        var state = offer(1, AugmentType.CAPS_LOCK);
        assertThatThrownBy(() -> augments.pick(roomId, ids.get(1), request(2))).isInstanceOf(AugmentConflictException.class);
        assertThat(state.hasSelected()).isFalse();
        augments.pick(roomId, ids.get(1), request(3));
        assertThat(state.getTargetUserId()).isEqualTo(ids.get(3));
        assertThat(augments.getOptions(roomId, ids.get(4)).targets()).filteredOn(target -> target.userId().equals(ids.get(2)))
                .allMatch(target -> !target.available());
    }
    @Test void confirmedSelectionIsImmutableAndOptionsAreStable() {
        var first = augments.getOptions(roomId, ids.get(0));
        assertThat(augments.getOptions(roomId, ids.get(0)).options()).isEqualTo(first.options());
        var type = first.options().getFirst().type();
        augments.pick(roomId, ids.get(0), new PickAugmentRequest(type, ids.get(1), first.roundId()));
        assertThatThrownBy(() -> augments.pick(roomId, ids.get(0), new PickAugmentRequest(type, ids.get(2), first.roundId())))
                .isInstanceOf(AugmentConflictException.class);
    }
    @Test void twoConcurrentSendersReserveOnlyOneRecipientAndLoserCanRetry() throws Exception {
        offer(0, AugmentType.CAPS_LOCK); offer(1, AugmentType.CAPS_LOCK);
        var results = race(() -> augments.pick(roomId, ids.get(0), request(2)),
                () -> augments.pick(roomId, ids.get(1), request(2)));
        assertThat(results).filteredOn(value -> value instanceof AugmentConflictException).hasSize(1);
        assertThat(states.inRoom(roomId).stream().filter(UserAugmentState::hasSelected).count()).isEqualTo(1);
        int loser = states.find(ids.get(0), roomId).orElseThrow().hasSelected() ? 1 : 0;
        augments.pick(roomId, ids.get(loser), request(3));
        assertThat(states.inRoom(roomId).stream().filter(UserAugmentState::hasSelected).count()).isEqualTo(2);
    }
    @Test void concurrentOptionReadsUseTheSameOffer() throws Exception {
        var results = race(() -> augments.getOptions(roomId, ids.get(0)), () -> augments.getOptions(roomId, ids.get(0)));
        assertThat(results.get(0)).isEqualToIgnoringGivenFields(results.get(1), "serverTime");
        assertThat(states.inRoom(roomId)).hasSize(1);
    }
    @Test void automaticAndManualPicksShareOneReservation() throws Exception {
        offer(0, AugmentType.CAPS_LOCK);
        race(() -> { augments.autoSelectIfNotSelected(roomId, ids.get(0)); return "auto"; },
                () -> augments.pick(roomId, ids.get(0), request(2)));
        assertThat(states.inRoom(roomId).stream().filter(UserAugmentState::hasSelected).count()).isEqualTo(1);
        assertThat(progress.findById(AugmentProgress.key(round().getRoundId(),
                states.find(ids.get(0), roomId).orElseThrow().getTargetUserId()))).isPresent();
    }
    @Test void initializationRaceAndOldSchedulerCannotAffectTheNewCycle() throws Exception {
        offer(0, AugmentType.CAPS_LOCK);
        String oldId = round().getRoundId();
        var oldDeadline = round().getPhaseEndsAt();
        race(() -> augments.pick(roomId, ids.get(0), new PickAugmentRequest(AugmentType.CAPS_LOCK, ids.get(1), oldId)),
                () -> { execution.run(roomId, () -> { rounds.advancePhase(roomId); rounds.advancePhase(roomId); }); return "reset"; });
        assertThat(round().getRoundId()).isNotEqualTo(oldId);
        assertThat(states.inRoom(roomId)).isEmpty();
        offer(0, AugmentType.CAPS_LOCK);
        assertThatThrownBy(() -> augments.pick(roomId, ids.get(0), new PickAugmentRequest(AugmentType.CAPS_LOCK, ids.get(1), oldId)))
                .isInstanceOf(AugmentConflictException.class);
        rounds.advancePhase(roomId, oldId, RoundPhase.PICKING, oldDeadline);
        assertThat(round().getPhase()).isEqualTo(RoundPhase.PICKING);
    }
    @Test void expiredManualPickIsRejectedButDeadlineAutoPickWorks() {
        var state = offer(0, AugmentType.CAPS_LOCK);
        ReflectionTestUtils.setField(round(), "phaseEndsAt", Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> augments.pick(roomId, ids.get(0), request(1))).isInstanceOf(AugmentConflictException.class);
        assertThat(state.hasSelected()).isFalse();
        rounds.advancePhase(roomId);
        assertThat(round().getPhase()).isEqualTo(RoundPhase.PLAYING);
        var selected = states.inRoom(roomId).stream().filter(UserAugmentState::hasSelected).toList();
        assertThat(selected).extracting(UserAugmentState::getTargetUserId).doesNotHaveDuplicates();
        assertThat(selected).allMatch(value -> !value.getUserId().equals(value.getTargetUserId()));
    }
    @Test void noOpponentOrOnlySelfAvailableSkipsAutomaticSelection() {
        execution.run(roomId, () -> {
            var room = rooms.findById(roomId).orElseThrow();
            ids.subList(1, ids.size()).forEach(room::removeMember);
        });
        augments.autoSelectIfNotSelected(roomId, ids.get(0));
        assertThat(augments.hasSelected(roomId, ids.get(0))).isFalse();
    }
    @Test void automaticPickSkipsWhenEveryOtherRecipientIsOccupied() {
        for (int sender = 1; sender < ids.size(); sender++) {
            pick(sender, sender == ids.size() - 1 ? 1 : sender + 1, AugmentType.CAPS_LOCK);
        }
        augments.autoSelectIfNotSelected(roomId, ids.get(0));
        assertThat(augments.hasSelected(roomId, ids.get(0))).isFalse();
        assertThat(states.inRoom(roomId).stream().filter(UserAugmentState::hasSelected).count()).isEqualTo(6);
    }
    @Test void databaseRollbackReleasesTheReservationWithoutConsumingThePick() {
        var state = offer(0, AugmentType.CAPS_LOCK);
        assertThatThrownBy(() -> execution.run(roomId, () -> {
            augments.pick(roomId, ids.get(0), request(1));
            throw new IllegalStateException("rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(state.hasSelected()).isFalse();
        assertThat(round().getScores().get(ids.get(0)).isAugmentUsed()).isFalse();
        assertThat(progress.findById(AugmentProgress.key(round().getRoundId(), ids.get(1)))).isEmpty();
        augments.pick(roomId, ids.get(0), request(1));
        assertThat(state.hasSelected()).isTrue();
    }
    @Test void noEffectLeaksToTheSelectorOrAnotherPlayer() {
        playing(AugmentType.CAPS_LOCK);
        assertThat(augments.applyAugment(roomId, ids.get(0), "hello")).isEqualTo("hello");
        assertThat(augments.applyAugment(roomId, ids.get(1), "hello")).isEqualTo("HELLO");
        assertThat(augments.applyAugment(roomId, ids.get(2), "hello")).isEqualTo("hello");
    }
    private List<Object> race(Callable<?> first, Callable<?> second) throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var tasks = List.of(first, second).stream().map(task -> executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                try { return task.call(); } catch (AugmentConflictException e) { return e; }
            })).toList();
            return List.of(tasks.get(0).get(15, TimeUnit.SECONDS), tasks.get(1).get(15, TimeUnit.SECONDS));
        }
    }
}
