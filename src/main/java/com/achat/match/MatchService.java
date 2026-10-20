package com.achat.match;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import com.achat.room.Room;
import com.achat.room.RoomStore;
import com.achat.round.RoundService;
import com.achat.room.RoomType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
@RequiredArgsConstructor
public class MatchService {

    private static final int MIN_PLAYERS = 6;
    private static final int ROUND_DURATION_MINUTES = 10;
    private static final long EXPAND_THRESHOLD_MS = 20000; // 20초 후 자동 확장

    private final MatchQueue matchQueue;
    private final RoomStore roomStore;
    private final RoundService roundService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.achat.common.RoomExecution execution;
    private final com.achat.security.AccountAccess access;
    private final com.achat.round.RoundStore roundStore;
    private final java.util.concurrent.ScheduledExecutorService starter =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "match-round-start"); thread.setDaemon(true); return thread;
            });
    private static final String LOCK = "system:matchmaking";

    private final Map<String, String> userMatches = new ConcurrentHashMap<>();

    public Map<String, Object> joinQueue(String userId, int preferredCount) {
        return execution.run(LOCK, () -> {
            access.active(userId);
            if (activeMatch(userId) != null) throw new com.achat.augment.AugmentConflictException("이미 진행 중인 매치에 참여하고 있습니다");
            checkpoint();
            return joinLocked(userId, preferredCount);
        });
    }
    private Map<String, Object> joinLocked(String userId, int preferredCount) {

        if (preferredCount < 6) preferredCount = 6;
        if (preferredCount > 12) preferredCount = 12;
        preferredCount = (preferredCount / 2) * 2; // 짝수로

        matchQueue.join(userId, preferredCount);

        return Map.of(
                "status", "waiting",
                "preferredCount", preferredCount,
                "queueSize", matchQueue.getQueueSize(),
                "estimatedCount", matchQueue.getOptimalPlayerCount()
        );
    }

    public void leaveQueue(String userId) {
        execution.run(LOCK, () -> { checkpoint(); matchQueue.leave(userId); });
    }

    public Map<String, Object> getQueueStatus(String userId) {
        return Map.of(
                "waiting", matchQueue.isWaiting(userId),
                "queueSize", matchQueue.getQueueSize(),
                "waitTime", matchQueue.getWaitTime(userId) / 1000,
                "estimatedCount", matchQueue.getOptimalPlayerCount()
        );
    }

    public String getMatchedRoom(String userId) {
        return execution.run(LOCK, () -> activeMatch(userId));
    }

    @Scheduled(fixedRate = 3000)
    public void processMatchmaking() {
        execution.run(LOCK, () -> { checkpoint(); processLocked(); });
    }
    private void processLocked() {
        int queueSize = matchQueue.getQueueSize();
        if (queueSize < MIN_PLAYERS) {
            return;
        }

        int optimalCount = matchQueue.getOptimalPlayerCount();
        long maxWait = matchQueue.getMaxWaitTime();

        // 20초 이상 대기 시 최소 인원(6명)으로 강제 매칭
        if (maxWait > EXPAND_THRESHOLD_MS && queueSize >= MIN_PLAYERS) {
            optimalCount = MIN_PLAYERS;
        }

        // 매칭 가능하면 실행
        if (queueSize >= optimalCount) {
            List<String> players = matchQueue.pollPlayers(optimalCount).stream().filter(id -> {
                try { access.active(id); return activeMatch(id) == null; }
                catch (com.achat.security.UnauthorizedException | org.springframework.security.access.AccessDeniedException e) { return false; }
            }).toList();
            if (players.size() >= MIN_PLAYERS) {
                createMatch(players);
            } else {
                // 부족하면 다시 큐에
                players.forEach(p -> matchQueue.join(p, 6));
            }
        }
    }

    private void createMatch(List<String> players) {
        String roomName = "매치 #" + UUID.randomUUID().toString().substring(0, 6);
        Room room = new Room(roomName, "SYSTEM", players.size());
        room.setType(RoomType.MATCH);

        players.forEach(room::addMember);
        roomStore.save(room);

        players.forEach(userId -> userMatches.put(userId, room.getId()));

        // 매칭 완료 알림
        com.achat.common.RoomExecution.afterCommit(() -> players.forEach(userId -> {
            messagingTemplate.convertAndSend("/topic/match/" + userId, Map.of(
                    "type", "MATCHED",
                    "roomId", room.getId(),
                    "players", players.size()
            ));
        }));

        // 3초 후 라운드 자동 시작
        com.achat.common.RoomExecution.afterCommit(() -> starter.schedule(() -> {
            try {
                roundService.startRoundAuto(room.getId(), ROUND_DURATION_MINUTES);
                messagingTemplate.convertAndSend("/topic/rooms/" + room.getId() + "/round",
                        roundService.getRoundState(room.getId()));
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(MatchService.class).warn("Match round start failed: {}", e.getClass().getSimpleName());
            }
        }, 3, java.util.concurrent.TimeUnit.SECONDS));
    }

    public void cleanupMatch(String roomId) {
        execution.run(LOCK, () -> { checkpoint(); userMatches.entrySet().removeIf(entry -> entry.getValue().equals(roomId)); });
    }
    private String activeMatch(String userId) {
        String id = userMatches.get(userId);
        if (id == null) return null;
        boolean ended = roundStore.findByRoomId(id).map(round -> round.getPhase() == com.achat.round.RoundPhase.ENDED).orElse(false);
        if (ended || roomStore.findById(id).filter(room -> room.hasMember(userId)).isEmpty()) {
            userMatches.remove(userId); return null;
        }
        return id;
    }
    private void checkpoint() {
        com.achat.common.RoomExecution.checkpoint(LOCK, () -> {
            Runnable queue = matchQueue.checkpoint();
            var matches = new java.util.HashMap<>(userMatches);
            return () -> { queue.run(); userMatches.clear(); userMatches.putAll(matches); };
        });
    }
    @jakarta.annotation.PreDestroy public void close() { starter.shutdownNow(); }
}
