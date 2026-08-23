package com.skillproof.common;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private record Window(long windowStartMillis, Deque<Long> hits) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int limit, long windowMillis) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, w) -> {
            if (w == null || now - w.windowStartMillis >= windowMillis) {
                return new Window(now, new ArrayDeque<>());
            }
            return w;
        });
        synchronized (window.hits()) {
            while (!window.hits().isEmpty() && now - window.hits().peekFirst() >= windowMillis) {
                window.hits().pollFirst();
            }
            if (window.hits().size() >= limit) {
                return false;
            }
            window.hits().addLast(now);
            return true;
        }
    }
}
