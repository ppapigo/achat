package com.achat.history;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.transaction.annotation.Transactional;

import com.achat.history.dto.GameHistoryResponse;
import com.achat.history.dto.UserStatsResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GameHistoryService {

    private final GameHistoryStore historyStore;

    /**
     * 게임 기록 저장 (매칭 종료 시 호출)
     */
    public void recordGame(String odUserId, String roomId, String roomName, boolean isMatchRoom,
                           int playerCount, int rank, int totalPoints, int messageCount,
                           int rpChange, int coinsEarned, int expEarned, String augmentUsed) {

        GameHistory history = new GameHistory(
                odUserId, roomId, roomName, isMatchRoom,
                playerCount, rank, totalPoints, messageCount,
                rpChange, coinsEarned, expEarned, augmentUsed
        );
        historyStore.save(history);

        log.info("게임 기록 저장 - user: {}, room: {}, rank: {}/{}, points: {}",
                odUserId, roomName, rank, playerCount, totalPoints);
    }

    /**
     * 전적 조회
     */
    public List<GameHistoryResponse> getHistory(String odUserId, int limit) {
        return historyStore.findRecentByUserId(odUserId, limit)
                .stream()
                .map(GameHistoryResponse::from)
                .toList();
    }

    /**
     * 통계 조회
     */
    public UserStatsResponse getStats(String odUserId) {
        UserStats stats = historyStore.calculateStats(odUserId);
        return UserStatsResponse.from(stats);
    }
}
