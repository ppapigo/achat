package com.achat.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Single-instance room serialization. Locks remain held until the database transaction completes. */
@Component
@RequiredArgsConstructor
public class RoomExecution {
    private final TransactionTemplate transactions;
    // Bounded stripes prevent arbitrary room IDs from growing a permanent lock map.
    private final ReentrantLock[] locks = java.util.stream.IntStream.range(0, 1024)
            .mapToObj(index -> new ReentrantLock(true)).toArray(ReentrantLock[]::new);

    public <T> T run(String roomId, Supplier<T> action) {
        ReentrantLock lock = locks[(roomId.hashCode() & Integer.MAX_VALUE) % locks.length];
        lock.lock();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { lock.unlock(); }
            });
            return action.get();
        }
        try {
            return transactions.execute(status -> action.get());
        } finally {
            lock.unlock();
        }
    }

    public void run(String roomId, Runnable action) {
        run(roomId, () -> { action.run(); return null; });
    }

    public static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { action.run(); }
        });
    }

    public static void afterRollback(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public int getOrder() { return Integer.MIN_VALUE; }
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) action.run();
            }
        });
    }

    /** Capture once per transaction, so nested service calls restore the original state. */
    public static void checkpoint(Object key, Supplier<Runnable> capture) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        boolean captured = TransactionSynchronizationManager.getSynchronizations().stream()
                .anyMatch(sync -> sync instanceof Checkpoint checkpoint && checkpoint.key.equals(key));
        if (!captured) TransactionSynchronizationManager.registerSynchronization(new Checkpoint(key, capture.get()));
    }

    private static final class Checkpoint implements TransactionSynchronization {
        private final Object key;
        private final Runnable restore;
        private Checkpoint(Object key, Runnable restore) { this.key = key; this.restore = restore; }
        @Override public int getOrder() { return Integer.MIN_VALUE; }
        @Override public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) restore.run();
        }
    }
}
