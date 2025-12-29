package com.achat.match;

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
public class MatchService {

    private static final int MIN_PLAYERS = 6;
    private static final int ROUND_DURATION_MINUTES = 10;
    private static final long EXPAND_THRESHOLD_MS = 20000; // 20초 후 자동 확장

    private final MatchQueue matchQueue;
    private final RoomStore roomStore;
    private final RoundService roundService;
    private final SimpMessagingTemplate messagingTemplate;

    private final Map<String, String> userMatches = new ConcurrentHashMap<>();

    public MatchService(MatchQueue matchQueue, RoomStore roomStore,
                        RoundService roundService, SimpMessagingTemplate messagingTemplate) {
        this.matchQueue = matchQueue;
        this.roomStore = roomStore;
        this.roundService = roundService;
        this.messagingTemplate = messagingTemplate;
    }

    public Map<String, Object> joinQueue(String userId, int preferredCount) {

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
        matchQueue.leave(userId);
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
        return userMatches.get(userId);
    }

    @Scheduled(fixedRate = 3000)
    @Scheduled(fixedRate = 3000)
    public void processMatchmaking() {
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
            List<String> players = matchQueue.pollPlayers(optimalCount);
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
        players.forEach(userId -> {
            messagingTemplate.convertAndSend("/topic/match/" + userId, Map.of(
                    "type", "MATCHED",
                    "roomId", room.getId(),
                    "players", players.size()
            ));
        });

        // 3초 후 라운드 자동 시작
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                roundService.startRoundAuto(room.getId(), ROUND_DURATION_MINUTES);
                messagingTemplate.convertAndSend("/topic/rooms/" + room.getId() + "/round",
                        roundService.getRoundState(room.getId()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void cleanupMatch(String roomId) {
        userMatches.entrySet().removeIf(entry -> entry.getValue().equals(roomId));
    }
}