package com.achat.security;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Bounded, atomic single-instance limiter. Proxy/IP handling is deliberately opt-in. */
@Service
public class RateLimitService {
    private record Window(long expiresAt, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    private static final int MAX_KEYS = 20_000;

    public synchronized void consume(String key, int limit, Duration duration) {
        long now = System.currentTimeMillis();
        Window window = windows.get(key);
        if (window == null || window.expiresAt <= now) {
            if (windows.size() >= MAX_KEYS) {
                windows.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
                if (windows.size() >= MAX_KEYS) throw new RateLimitedException(60);
            }
            window = new Window(now + duration.toMillis(), 0);
        }
        if (window.count >= limit) throw new RateLimitedException(Math.max(1, (window.expiresAt - now + 999) / 1000));
        windows.put(key, new Window(window.expiresAt, window.count + 1));
    }
    public static class RateLimitedException extends RuntimeException {
        public final long retryAfter;
        RateLimitedException(long retryAfter) { super("요청이 너무 많습니다. 잠시 후 다시 시도해 주세요"); this.retryAfter = retryAfter; }
    }
}
