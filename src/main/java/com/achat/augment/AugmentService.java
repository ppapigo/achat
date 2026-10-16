package com.achat.augment;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.augment.dto.AugmentOptionResponse;
import com.achat.augment.dto.PickAugmentRequest;
import com.achat.augment.effect.AugmentEffectRegistry;
import com.achat.job.JobStore;
import com.achat.job.JobType;
import com.achat.room.Room;
import com.achat.room.RoomStore;
import com.achat.user.UserStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@Transactional
@RequiredArgsConstructor
public class AugmentService {

    private final AugmentStore augmentStore;
    private final RoomStore roomStore;
    private final AugmentEffectRegistry effectRegistry;
    private final JobStore jobStore;
    private final UserStore userStore;
    private final Random random = new Random();  // 추가

    public AugmentOptionResponse getOptions(String roomId, String userId) {
        Room room = requireMember(roomId, userId);
        var stateOpt = augmentStore.find(userId, roomId).filter(state -> !state.isExpired());
        if (stateOpt.isPresent()) {
            return toResponse(stateOpt.get(), room);
        }

        boolean isFreelancer = jobStore.getOrCreate(userId).getCurrentJob() == JobType.FREELANCER;
        int optionCount = isFreelancer ? 4 : 3;

        List<AugmentType> availableTypes = new ArrayList<>(List.of(
                AugmentType.TURBO_CASTER,
                AugmentType.ECHO_BURST,
                AugmentType.VISUAL_SPELL,
                AugmentType.THREAD_WEAVER,
                AugmentType.SHADOW_SPEAKER,
                AugmentType.SPOTLIGHT,
                AugmentType.LINK_BRIDGE,
                AugmentType.HARMONY_AURA,
                AugmentType.CHAOS_ROULETTE,
                AugmentType.QUEST_GIVER,
                AugmentType.TIME_SHIFTER,
                AugmentType.INSIGHT_DROP
        ));

        Collections.shuffle(availableTypes);
        List<AugmentType> options = availableTypes.subList(0, Math.min(optionCount, availableTypes.size()));

        UserAugmentState newState = new UserAugmentState(userId, roomId, options);
        augmentStore.save(newState);

        return toResponse(newState, room);
    }

    public AugmentOptionResponse pick(String roomId, String userId, PickAugmentRequest request) {
        Room room = requireMember(roomId, userId);
        if (request.targetUserId() == null || request.targetUserId().equals(userId)
                || !room.hasMember(request.targetUserId())) {
            throw new IllegalArgumentException("같은 방에 있는 상대를 선택해주세요");
        }

        UserAugmentState state = augmentStore.find(userId, roomId)
                .orElseThrow(() -> new IllegalArgumentException("먼저 옵션을 조회해주세요"));

        state.select(request.type(), request.targetUserId());

        return toResponse(state, room);
    }

    /**
     * 증강 미선택 유저 자동 선택 (PICKING 종료 시 호출)
     */
    public void autoSelectIfNotSelected(String roomId, String userId) {
        Room room = requireMember(roomId, userId);
        List<String> targets = room.getMemberIds().stream().filter(id -> !id.equals(userId)).toList();
        if (targets.isEmpty()) return;

        var stateOpt = augmentStore.find(userId, roomId).filter(state -> !state.isExpired());

        if (stateOpt.isEmpty()) {
            // 옵션 조회도 안 했으면 먼저 생성
            getOptions(roomId, userId);
            stateOpt = augmentStore.find(userId, roomId);
        }

        UserAugmentState state = stateOpt.orElse(null);
        if (state == null) return;

        // 이미 선택했으면 패스
        if (state.hasSelected()) return;

        // 랜덤 자동 선택
        List<AugmentType> options = state.getOptions();
        if (!options.isEmpty()) {
            AugmentType randomPick = options.get(random.nextInt(options.size()));
            state.select(randomPick, targets.get(random.nextInt(targets.size())));
            augmentStore.save(state);
            System.out.println("자동 증강 선택: " + userId + " -> " + randomPick);
        }
    }

    /**
     * 유저가 증강을 선택했는지 확인
     */
    public boolean hasSelected(String roomId, String userId) {
        return augmentStore.find(userId, roomId)
                .map(UserAugmentState::hasSelected)
                .orElse(false);
    }

    public String applyAugment(String roomId, String userId, String message) {
        return applyAugments(roomId, userId, message, getAppliedAugmentTypes(roomId, userId));
    }

    public String applyAugments(String roomId, String userId, String message, List<AugmentType> types) {
        AugmentContext context = new AugmentContext(userId, roomId, message);
        String result = message;
        for (AugmentType type : types) {
            result = effectRegistry.applyEffect(type, result, context);
        }
        return result;
    }

    /**
     * 선택한 증강은 지정한 상대 한 명에게만 적용한다.
     * 같은 종류는 한 번만 적용하고, 효과 순서는 항상 일정하게 유지한다.
     */
    public List<AugmentType> getAppliedAugmentTypes(String roomId, String userId) {
        var room = roomStore.findById(roomId).orElse(null);
        if (room == null || !room.hasMember(userId)) {
            return List.of();
        }

        return room.getMemberIds().stream()
                .filter(memberId -> !memberId.equals(userId))
                .flatMap(memberId -> augmentStore.find(memberId, roomId).stream())
                .filter(state -> !state.isExpired())
                .filter(UserAugmentState::hasSelected)
                .filter(state -> userId.equals(state.getTargetUserId()))
                .map(UserAugmentState::getSelectedAugment)
                .distinct()
                .sorted()
                .toList();
    }

    private Room requireMember(String roomId, String userId) {
        Room room = roomStore.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));
        if (!room.hasMember(userId)) {
            throw new IllegalArgumentException("방에 참여한 후 증강을 선택해주세요");
        }
        return room;
    }

    private AugmentOptionResponse toResponse(UserAugmentState state, Room room) {
        List<AugmentOptionResponse.Target> targets = room.getMemberIds().stream()
                .filter(id -> !id.equals(state.getUserId()))
                .sorted()
                .map(id -> new AugmentOptionResponse.Target(id, userStore.findById(id)
                        .map(user -> user.getUsername()).orElse(id)))
                .toList();
        return new AugmentOptionResponse(
                state.getOptions().stream().map(AugmentDefinition::of).toList(),
                state.getSelectedAugment(), state.getTargetUserId(), targets);
    }
}
