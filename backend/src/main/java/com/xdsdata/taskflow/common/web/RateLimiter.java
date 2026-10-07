package com.xdsdata.taskflow.common.web;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.xdsdata.taskflow.common.AppProperties;
import com.xdsdata.taskflow.common.error.TooManyRequestsException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fixed-window, in-memory rate limiter for sensitive endpoints (login, password reset). Suitable for a
 * single application instance; a shared store would be needed when scaling out.
 */
@Component
public class RateLimiter {

	private static final Duration MAX_WINDOW = Duration.ofHours(1);

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	private final Clock clock;

	private final boolean enabled;

	public RateLimiter(Clock clock, AppProperties properties) {
		this.clock = clock;
		this.enabled = properties.rateLimitEnabled();
	}

	/** Records an attempt and reports whether it is within {@code limit} attempts per {@code window}. */
	public boolean tryAcquire(String key, int limit, Duration window) {
		if (!enabled) {
			return true;
		}
		long now = clock.millis();
		Window current = windows.compute(key, (k, existing) -> existing == null || now - existing.start() >= window.toMillis()
				? new Window(now, 1) : new Window(existing.start(), existing.count() + 1));
		return current.count() <= limit;
	}

	/** Like {@link #tryAcquire} but rejects the request with 429 when the limit is exceeded. */
	public void check(String key, int limit, Duration window) {
		if (!tryAcquire(key, limit, window)) {
			throw new TooManyRequestsException();
		}
	}

	@Scheduled(fixedDelay = 10, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
	void evictExpiredWindows() {
		long cutoff = clock.millis() - MAX_WINDOW.toMillis();
		windows.values().removeIf(window -> window.start() < cutoff);
	}

	private record Window(long start, int count) {
	}

}
