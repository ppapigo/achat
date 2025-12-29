package com.achat.augment;

import com.achat.augment.dto.AugmentOptionResponse;
import com.achat.augment.dto.PickAugmentRequest;
import com.achat.augment.effect.AugmentEffectRegistry;
import com.achat.job.JobStore;
import com.achat.job.JobType;
import com.achat.room.RoomStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class AugmentService {

    private final AugmentStore augmentStore;
    private final RoomStore roomStore;
    private final AugmentEffectRegistry effectRegistry;
    private final JobStore jobStore;
    private final Random random = new Random();  // 추가

    public AugmentService(AugmentStore augmentStore, RoomStore roomStore,
                          AugmentEffectRegistry effectRegistry, JobStore jobStore) {
        this.augmentStore = augmentStore;
        this.roomStore = roomStore;
        this.effectRegistry = effectRegistry;
        this.jobStore = jobStore;
    }

    public AugmentOptionResponse getOptions(String roomId, String userId) {
        var stateOpt = augmentStore.find(userId, roomId);
        if (stateOpt.isPresent() && stateOpt.get().getSelectedAugment() != null) {
            UserAugmentState state = stateOpt.get();
            return new AugmentOptionResponse(
                    state.getOptions().stream().map(AugmentDefinition::of).toList(),
                    state.getSelectedAugment()
            );
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

        return new AugmentOptionResponse(
                options.stream().map(AugmentDefinition::of).toList(),
                null
        );
    }

    public AugmentOptionResponse pick(String roomId, String userId, PickAugmentRequest request) {
        roomStore.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));

        UserAugmentState state = augmentStore.find(userId, roomId)
                .orElseThrow(() -> new IllegalArgumentException("먼저 옵션을 조회해주세요"));

        state.select(request.type());

        return new AugmentOptionResponse(
                state.getOptions().stream().map(AugmentDefinition::of).toList(),
                state.getSelectedAugment()
        );
    }

    /**
     * 증강 미선택 유저 자동 선택 (PICKING 종료 시 호출)
     */
    public void autoSelectIfNotSelected(String roomId, String userId) {
        var stateOpt = augmentStore.find(userId, roomId);

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
            state.select(randomPick);
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
        var stateOpt = augmentStore.find(userId, roomId)
                .filter(s -> !s.isExpired())
                .filter(UserAugmentState::hasSelected);

        if (stateOpt.isEmpty()) {
            return message;
        }

        UserAugmentState state = stateOpt.get();
        AugmentContext context = new AugmentContext(userId, roomId, message);
        return effectRegistry.applyEffect(state.getSelectedAugment(), message, context);
    }

    public String getSelectedAugmentType(String roomId, String userId) {
        return augmentStore.find(userId, roomId)
                .filter(s -> !s.isExpired())
                .filter(UserAugmentState::hasSelected)
                .map(state -> state.getSelectedAugment().name())
                .orElse(null);
    }
}