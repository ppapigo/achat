package com.achat.round;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

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

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class RoundService {

    private final RoundStore roundStore;
    private final RoomStore roomStore;
    private final AugmentStore augmentStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoundScheduler roundScheduler;
    private final UserStore userStore;
    private final RankService rankService;
    private final AugmentService augmentService;
    private final GameHistoryService gameHistoryService;

    public RoundResponse startRound(String roomId, String odLRXnwB, StartRoundRequest request) {
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

    public RoundResponse startRoundAuto(String roomId, int durationMinutes) {
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

    public void advancePhase(String roomId) {
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

    public boolean onMessage(String roomId, String odLRXnwB, String content) {
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

    public void onAugmentUsed(String roomId, String odLRXnwB) {
        roundStore.findByRoomId(roomId).ifPresent(round -> {
            round.recordAugmentUsed(odLRXnwB);
            roundStore.save(round);
        });
    }

    public void onUserJoin(String roomId, String odLRXnwB) {
        roundStore.findByRoomId(roomId).ifPresent(round -> {
            round.addParticipant(odLRXnwB);
            roundStore.save(round);
        });
    }

    public RoundResponse getRoundState(String roomId) {
        return roundStore.findByRoomId(roomId)
                .map(RoundResponse::from)
                .orElse(RoundResponse.waiting(roomId));
    }

    private void clearAllAugmentStates(Room room, String roomId) {
        room.getMemberIds().forEach(memberId -> augmentStore.remove(memberId, roomId));
    }

    private void broadcastRoundState(Round round) {
        RoundResponse response = RoundResponse.from(round);
        messagingTemplate.convertAndSend("/topic/rooms/" + round.getRoomId() + "/round", response);
    }

    private void broadcastRoundEnd(Round round, Map<String, RankService.RpResult> rpResults) {
        RoundEndResponse response = RoundEndResponse.from(round, rpResults);
        messagingTemplate.convertAndSend("/topic/rooms/" + round.getRoomId() + "/round/end", response);
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
