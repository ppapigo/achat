package com.achat.round;

import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

@Component
@Slf4j
public class RoundScheduler {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private RoundService roundService;

    @org.springframework.beans.factory.annotation.Autowired
    public void setRoundService(@org.springframework.context.annotation.Lazy RoundService roundService) {
        this.roundService = roundService;
    }

    public void scheduleNextPhase(Round round) {
        com.achat.common.RoomExecution.afterCommit(() -> scheduleCommittedPhase(round));
    }

    private void scheduleCommittedPhase(Round round) {
        if (round.getPhaseEndsAt() == null) return;

        cancelScheduledTask(round.getRoomId());

        long delayMs = Duration.between(Instant.now(), round.getPhaseEndsAt()).toMillis();
        if (delayMs < 0) delayMs = 0;

        log.info("라운드 스케줄: roomId={}, phase={}, {}ms 후 진행",
                round.getRoomId(), round.getPhase(), delayMs);

        String roundId = round.getRoundId();
        RoundPhase phase = round.getPhase();
        Instant deadline = round.getPhaseEndsAt();
        ScheduledFuture<?> future = scheduler.schedule(
                () -> advanceSafely(round.getRoomId(), roundId, phase, deadline),
                delayMs,
                TimeUnit.MILLISECONDS
        );

        scheduledTasks.put(round.getRoomId(), future);
    }

    private void advanceSafely(String roomId, String roundId, RoundPhase phase, Instant deadline) {
        try {
            roundService.advancePhase(roomId, roundId, phase, deadline);
        } catch (Exception e) {
            log.error("라운드 진행 오류: {}. 1초 후 재시도합니다.", e.getMessage(), e);
            if (!scheduler.isShutdown()) {
                scheduledTasks.put(roomId, scheduler.schedule(
                        () -> advanceSafely(roomId, roundId, phase, deadline), 1, TimeUnit.SECONDS));
            }
        }
    }

    public void cancelScheduledTask(String roomId) {
        ScheduledFuture<?> existing = scheduledTasks.remove(roomId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("RoundScheduler 종료 중...");
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("RoundScheduler 종료 완료");
    }
}
