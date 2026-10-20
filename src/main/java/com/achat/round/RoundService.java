package com.achat.round;

import lombok.RequiredArgsConstructor;

import com.achat.augment.AugmentService;
import com.achat.augment.AugmentStore;
import com.achat.history.GameHistoryService;
import com.achat.rank.RankService;
import com.achat.round.dto.RoundEndResponse;
import com.achat.round.dto.RoundResponse;
import com.achat.round.dto.StartRoundRequest;
import com.achat.room.Room;
import com.achat.room.RoomStore;
import com.achat.user.UserStore;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.achat.common.RoomExecution;
import com.achat.augment.AugmentAwardRepository;
import com.achat.augment.effect.MessageEffectService;
import com.achat.quest.QuestStore;
import java.time.Instant;
import java.util.Objects;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoundService {

    private final RoomExecution execution;
    private final MessageEffectService effects;
    private final AugmentAwardRepository awards;
    private final QuestStore quests;
    private final RoundStore roundStore;
    private final RoomStore roomStore;
    private final AugmentStore augmentStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoundScheduler roundScheduler;
    private final UserStore userStore;
    private final RankService rankService;
    private final AugmentService augmentService;
    private final GameHistoryService gameHistoryService;

    public RoundResponse startRound(String roomId, String userId, StartRoundRequest request) {
        return execution.run(roomId, () -> startRoundLocked(roomId, userId, request));
    }
    public RoundResponse startRoundAuto(String roomId, int minutes) {
        return execution.run(roomId, () -> startRoundAutoLocked(roomId, minutes));
    }
    public void advancePhase(String roomId) {
        execution.run(roomId, () -> advancePhaseLocked(roomId));
    }
    public void advancePhase(String roomId, String roundId, RoundPhase phase, Instant deadline) {
        execution.run(roomId, () -> {
            var current = roundStore.findByRoomId(roomId).orElse(null);
            if (current != null && Objects.equals(current.getRoundId(), roundId)
                    && current.getPhase() == phase && Objects.equals(current.getPhaseEndsAt(), deadline)
                    && !Instant.now().isBefore(deadline)) advancePhaseLocked(roomId);
        });
    }
    public boolean onMessage(String roomId, String userId, String content) {
        return execution.run(roomId, () -> onMessageLocked(roomId, userId, content));
    }
    public void onAugmentUsed(String roomId, String userId) {
        execution.run(roomId, () -> onAugmentUsedLocked(roomId, userId));
    }
    public void onUserJoin(String roomId, String userId) {
        execution.run(roomId, () -> onUserJoinLocked(roomId, userId));
    }
    public RoundResponse getRoundState(String roomId) {
        return execution.run(roomId, () -> getRoundStateLocked(roomId));
    }
    private void updatePoints(Round round) {
        checkpoint(round.getRoomId());
        round.getScores().values().forEach(score -> score.setEffectPoints(0));
        awards.totals(round.getGameId()).forEach(row -> {
            var score = round.getScores().get((String) row[0]);
            if (score != null) score.setEffectPoints(((Number) row[1]).intValue());
        });
        round.getScores().values().forEach(RoundScore::calculate);
    }

    private RoundResponse startRoundLocked(String roomId, String odLRXnwB, StartRoundRequest request) {
        checkpoint(roomId);
        Room room = roomStore.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));

        if (!room.getOwnerId().equals(odLRXnwB)) {
            throw new IllegalArgumentException("방장만 라운드를 시작할 수 있습니다");
        }

        if (!request.isValidDuration()) {
            throw new IllegalArgumentException("허용된 시간: 10, 15, 20, 30분");
        }

        Round existingRound = roundStore.findByRoomId(roomId).orElse(null);
        if (existingRound != null && existingRound.getPhase() != RoundPhase.WAITING
                && existingRound.getPhase() != RoundPhase.ENDED) {
            throw new IllegalArgumentException("이미 진행 중인 라운드가 있습니다");
        }

        clearAllAugmentStates(room, roomId);

        Round round = new Round(roomId, request.durationMinutes());
        room.getMemberIds().forEach(round::addParticipant);
        round.startRound();
        roundStore.save(round);

        broadcastRoundState(round);
        roundScheduler.scheduleNextPhase(round);

        return RoundResponse.from(round);
    }

    private RoundResponse startRoundAutoLocked(String roomId, int durationMinutes) {
        checkpoint(roomId);
        Room room = roomStore.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다"));

        Round existingRound = roundStore.findByRoomId(roomId).orElse(null);
        if (existingRound != null && existingRound.getPhase() != RoundPhase.WAITING
                && existingRound.getPhase() != RoundPhase.ENDED) {
            throw new IllegalArgumentException("이미 진행 중인 라운드가 있습니다");
        }

        clearAllAugmentStates(room, roomId);

        Round round = new Round(roomId, durationMinutes);
        room.getMemberIds().forEach(round::addParticipant);
        round.startRound();
        roundStore.save(round);

        broadcastRoundState(round);
        roundScheduler.scheduleNextPhase(round);

        return RoundResponse.from(round);
    }

    private void advancePhaseLocked(String roomId) {
        checkpoint(roomId);
        Round round = roundStore.findByRoomId(roomId).orElse(null);
        if (round == null) return;

        Room room = roomStore.findById(roomId).orElse(null);
        if (room == null) return;

        switch (round.getPhase()) {
            case PICKING -> {
                room.getMemberIds().forEach(memberId ->
                        augmentService.autoSelectIfNotSelected(roomId, memberId)
                );

                round.startPlaying();
                roundStore.save(round);
                broadcastRoundState(round);
                roundScheduler.scheduleNextPhase(round);
            }
            case PLAYING -> {
                effects.endCycle(roomId, round.getRoundId());
                quests.cancelRoom(roomId);
                updatePoints(round);
                if (round.isLastPhase()) {
                    round.end();
                    roundStore.save(round);

                    applyRewards(round);

                    Map<String, RankService.RpResult> rpResults = Map.of();
                    if (room.isMatchRoom()) {
                        rpResults = rankService.applyRoundResults(round);
                        System.out.println("랭크 포인트 적용 (임시방): " + roomId);
                    }

                    // 게임 기록 저장
                    saveGameHistory(round, room, rpResults);

                    broadcastRoundEnd(round, rpResults);
                    broadcastRoundState(round);

                    augmentStore.clearRoom(roomId);
                    round.reset();
                    roundStore.save(round);
                    broadcastRoundState(round);
                } else {
                    clearAllAugmentStates(room, roomId);
                    round.startPicking();
                    roundStore.save(round);
                    broadcastRoundState(round);
                    roundScheduler.scheduleNextPhase(round);
                }
            }
            default -> {}
        }
    }

    private void applyRewards(Round round) {
        round.getRewards().forEach((odLRXnwB, reward) -> {
            userStore.findById(odLRXnwB).ifPresent(user -> {
                user.addReward(reward.getCoins(), reward.getExp());
                userStore.save(user);
                System.out.println("보상 지급: " + odLRXnwB + " -> "
                        + reward.getCoins() + "코인, " + reward.getExp() + "exp");
            });
        });
    }

    private boolean onMessageLocked(String roomId, String odLRXnwB, String content) {
        checkpoint(roomId);
        Round round = roundStore.findByRoomId(roomId).orElse(null);
        if (round == null) {
            return false;
        }

        boolean scored = round.processMessage(odLRXnwB, content);
        if (scored) {
            roundStore.save(round);
        }
        return scored;
    }

    private void onAugmentUsedLocked(String roomId, String odLRXnwB) {
        checkpoint(roomId);
        roundStore.findByRoomId(roomId).ifPresent(round -> {
            round.recordAugmentUsed(odLRXnwB);
            roundStore.save(round);
        });
    }

    private void onUserJoinLocked(String roomId, String odLRXnwB) {
        checkpoint(roomId);
        roundStore.findByRoomId(roomId).ifPresent(round -> {
            round.addParticipant(odLRXnwB);
            roundStore.save(round);
        });
    }

    private RoundResponse getRoundStateLocked(String roomId) {
        return roundStore.findByRoomId(roomId)
                .map(round -> { updatePoints(round); return RoundResponse.from(round); })
                .orElse(RoundResponse.waiting(roomId));
    }

    private void clearAllAugmentStates(Room room, String roomId) {
        augmentStore.clearRoom(roomId);
    }

    private void checkpoint(String roomId) {
        roundStore.checkpoint(roomId);
    }

    private void broadcastRoundState(Round round) {
        updatePoints(round);
        RoundResponse response = RoundResponse.from(round);
        RoomExecution.afterCommit(() -> messagingTemplate.convertAndSend("/topic/rooms/" + round.getRoomId() + "/round", response));
    }

    private void broadcastRoundEnd(Round round, Map<String, RankService.RpResult> rpResults) {
        RoundEndResponse response = RoundEndResponse.from(round, rpResults);
        RoundEndResponse complete = new RoundEndResponse(response.roomId(), response.results(),
                effects.highlights(round.getRoomId(), round.getGameId()));
        RoomExecution.afterCommit(() -> messagingTemplate.convertAndSend("/topic/rooms/" + round.getRoomId() + "/round/end", complete));
    }

    private void saveGameHistory(Round round, Room room, Map<String, RankService.RpResult> rpResults) {
        Map<String, RoundScore> scores = round.getScores();
        int playerCount = scores.size();

        List<Map.Entry<String, RoundScore>> ranked = scores.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getTotalPoints(), a.getValue().getTotalPoints()))
                .toList();

        for (int i = 0; i < ranked.size(); i++) {
            String odLRXnwB = ranked.get(i).getKey();
            RoundScore score = ranked.get(i).getValue();
            int rank = i + 1;

            int rpChange = 0;
            if (rpResults.containsKey(odLRXnwB)) {
                rpChange = rpResults.get(odLRXnwB).rpChange();  // ⭐ 여기 수정!
            }

            RoundReward reward = round.getRewards().get(odLRXnwB);
            int coinsEarned = reward != null ? reward.getCoins() : 0;
            int expEarned = reward != null ? reward.getExp() : 0;

            String augmentUsed = null;

            gameHistoryService.recordGame(
                    odLRXnwB,
                    room.getId(),
                    room.getName(),
                    room.isMatchRoom(),
                    playerCount,
                    rank,
                    score.getTotalPoints(),
                    score.getMessageCount(),
                    rpChange,
                    coinsEarned,
                    expEarned,
                    augmentUsed
            );
        }

        System.out.println("게임 기록 저장 완료: " + room.getName() + " (" + playerCount + "명)");
    }
}
