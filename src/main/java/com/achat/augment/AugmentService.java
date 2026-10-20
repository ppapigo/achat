package com.achat.augment;

import com.achat.augment.dto.*;
import com.achat.augment.effect.AugmentEffectRegistry;
import com.achat.common.RoomExecution;
import com.achat.job.*;
import com.achat.room.*;
import com.achat.round.*;
import com.achat.user.UserStore;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AugmentService {
    private final AugmentStore augmentStore;
    private final RoomStore roomStore;
    private final AugmentEffectRegistry effectRegistry;
    private final JobStore jobStore;
    private final UserStore userStore;
    private final RoundStore rounds;
    private final RoomExecution execution;
    private final AugmentProgressRepository progress;
    private final AugmentRandom random;
    private final SimpMessagingTemplate messaging;

    public AugmentOptionResponse getOptions(String roomId, String userId) {
        return execution.run(roomId, () -> {
            Room room = requireMember(roomId, userId);
            Round round = rounds.findByRoomId(roomId).orElse(null);
            UserAugmentState state = round == null ? null : augmentStore.find(userId, roomId)
                    .filter(value -> Objects.equals(value.getRoundId(), round.getRoundId())).orElse(null);
            if (state == null && picking(round)) state = offer(roomId, userId, round);
            return response(state, room, round, userId);
        });
    }

    public AugmentOptionResponse pick(String roomId, String userId, PickAugmentRequest request) {
        return execution.run(roomId, () -> {
            Room room = requireMember(roomId, userId);
            Round round = rounds.findByRoomId(roomId).orElse(null);
            if (!picking(round) || !Objects.equals(round.getRoundId(), request.roundId())) {
                throw new AugmentConflictException("선택 시간이 끝났거나 이전 라운드의 요청입니다. 목록을 새로고침해주세요");
            }
            UserAugmentState state = augmentStore.find(userId, roomId)
                    .orElseThrow(() -> new IllegalArgumentException("먼저 옵션을 조회해주세요"));
            choose(room, round, state, request.type(), request.targetUserId());
            return response(state, room, round, userId);
        });
    }

    /** Called at PICKING -> PLAYING with the same room lock. Deadline is intentionally allowed. */
    public void autoSelectIfNotSelected(String roomId, String userId) {
        execution.run(roomId, () -> {
            Room room = requireMember(roomId, userId);
            Round round = rounds.findByRoomId(roomId).orElse(null);
            if (round == null || round.getPhase() != RoundPhase.PICKING) return;
            UserAugmentState state = offer(roomId, userId, round);
            if (state.hasSelected()) return;
            List<String> targets = room.getMemberIds().stream().filter(id -> !id.equals(userId))
                    .filter(id -> augmentStore.received(roomId, round.getRoundId(), id).isEmpty()).toList();
            if (targets.isEmpty()) return;
            choose(room, round, state, state.getOptions().get(random.nextInt(state.getOptions().size())),
                    targets.get(random.nextInt(targets.size())));
        });
    }

    private UserAugmentState offer(String roomId, String userId, Round round) {
        var existing = augmentStore.find(userId, roomId);
        if (existing.isPresent()) return existing.get();
        int count = jobStore.getOrCreate(userId).getCurrentJob() == JobType.FREELANCER ? 4 : 3;
        List<AugmentType> types = new ArrayList<>(List.of(AugmentType.values()));
        Collections.shuffle(types);
        return augmentStore.saveIfAbsent(new UserAugmentState(userId, roomId, round.getRoundId(),
                types.subList(0, count), round.getPhaseEndsAt()));
    }

    private void choose(Room room, Round round, UserAugmentState state, AugmentType type, String target) {
        if (!Objects.equals(state.getRoundId(), round.getRoundId())) throw new AugmentConflictException("이전 라운드의 선택입니다");
        if (target == null || target.equals(state.getUserId()) || !room.hasMember(target)) {
            throw new IllegalArgumentException("같은 방에 있는 상대를 선택해주세요");
        }
        if (state.hasSelected()) throw new AugmentConflictException("이미 증강을 선택했습니다");
        if (!state.getOptions().contains(type)) throw new IllegalArgumentException("선택 가능한 증강이 아닙니다");
        if (augmentStore.received(room.getId(), round.getRoundId(), target).isPresent()) {
            throw new AugmentConflictException("이미 증강을 받은 상대입니다. 다른 상대를 선택해주세요");
        }
        String word = type == AugmentType.FORBIDDEN_WORD
                ? AugmentPolicy.SAFE_WORDS.get(random.nextInt(AugmentPolicy.SAFE_WORDS.size())) : null;
        progress.saveAndFlush(new AugmentProgress(room.getId(), round.getRoundId(), target, type, word));
        augmentStore.select(state, type, target);
        RoomExecution.afterRollback(state::undoSelection);
        rounds.checkpoint(room.getId());
        round.recordAugmentUsed(state.getUserId());
        round.getScores().values().forEach(RoundScore::calculate);
        var roundState = com.achat.round.dto.RoundResponse.from(round);
        String cycleId = round.getRoundId();
        RoomExecution.afterCommit(() -> {
            messaging.convertAndSend("/topic/rooms/" + room.getId() + "/augments",
                    Map.of("roundId", cycleId, "type", "UPDATED", "serverTime", Instant.now()));
            messaging.convertAndSend("/topic/rooms/" + room.getId() + "/round", roundState);
        });
    }

    public boolean hasSelected(String roomId, String userId) {
        return execution.run(roomId, () -> augmentStore.find(userId, roomId).map(UserAugmentState::hasSelected).orElse(false));
    }

    public String applyAugment(String roomId, String userId, String message) {
        return applyAugments(roomId, userId, message, getAppliedAugmentTypes(roomId, userId));
    }

    public String applyAugments(String roomId, String userId, String message, List<AugmentType> types) {
        String result = message;
        for (AugmentType type : types) result = effectRegistry.applyEffect(type, result, new AugmentContext(userId, roomId, message));
        return result;
    }

    public List<AugmentType> getAppliedAugmentTypes(String roomId, String userId) {
        return execution.run(roomId, () -> {
            Room room = roomStore.findById(roomId).orElse(null);
            Round round = rounds.findByRoomId(roomId).orElse(null);
            if (room == null || !room.hasMember(userId) || round == null || round.getPhase() != RoundPhase.PLAYING
                    || round.getPhaseEndsAt() == null || !Instant.now().isBefore(round.getPhaseEndsAt())) return List.of();
            return augmentStore.received(roomId, round.getRoundId(), userId)
                    .map(state -> List.of(state.getSelectedAugment())).orElse(List.of());
        });
    }

    private boolean picking(Round round) {
        return round != null && round.getPhase() == RoundPhase.PICKING
                && round.getPhaseEndsAt() != null && Instant.now().isBefore(round.getPhaseEndsAt());
    }

    private Room requireMember(String roomId, String userId) {
        Room room = roomStore.findById(roomId).orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));
        if (!room.hasMember(userId)) throw new IllegalArgumentException("방에 참여한 후 증강을 선택해주세요");
        return room;
    }

    private AugmentOptionResponse response(UserAugmentState state, Room room, Round round, String userId) {
        String roundId = round == null ? null : round.getRoundId();
        var received = roundId == null ? Optional.<AugmentProgress>empty()
                : progress.findById(AugmentProgress.key(roundId, userId));
        var targets = room.getMemberIds().stream().filter(id -> !id.equals(userId)).sorted()
                .map(id -> new AugmentOptionResponse.Target(id,
                        userStore.findById(id).map(user -> user.getUsername()).orElse("참여자"),
                        roundId != null && augmentStore.received(room.getId(), roundId, id).isEmpty())).toList();
        return new AugmentOptionResponse(state == null ? List.of() : state.getOptions().stream().map(AugmentDefinition::of).toList(),
                state == null ? null : state.getSelectedAugment(), state == null ? null : state.getTargetUserId(),
                targets, roundId, state == null ? null : state.getExpiresAt(), Instant.now(),
                picking(round) && (state == null || !state.hasSelected()),
                received.map(AugmentProgress::getType).orElse(null), received.map(AugmentProgress::getForbiddenWord).orElse(null));
    }
}
