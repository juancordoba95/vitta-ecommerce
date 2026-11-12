package com.vitta.backend.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vitta.backend.auth.exception.TooManyLoginAttemptsException;
import org.springframework.stereotype.Component;

/**
 * In-memory, per-client fixed-window limiter for failed logins: after {@value #MAX_FAILURES}
 * failures within {@link #WINDOW}, further attempts are rejected until the window ends. State
 * is per instance and lost on restart, which is fine for a single-node deployment.
 */
@Component
public class LoginAttemptLimiter {

	public static final int MAX_FAILURES = 5;

	public static final Duration WINDOW = Duration.ofMinutes(1);

	/** Above this many tracked clients, expired windows are purged on each failure. */
	private static final int PURGE_THRESHOLD = 1_000;

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	private final Clock clock;

	public LoginAttemptLimiter(Clock clock) {
		this.clock = clock;
	}

	/**
	 * @throws TooManyLoginAttemptsException if the client already reached the limit
	 */
	public void checkAllowed(String clientKey) {
		Instant now = clock.instant();
		Window window = windows.get(clientKey);
		if (window != null && !window.isExpired(now) && window.failures() >= MAX_FAILURES) {
			throw new TooManyLoginAttemptsException(Duration.between(now, window.end()));
		}
	}

	public void recordFailure(String clientKey) {
		Instant now = clock.instant();
		windows.compute(clientKey,
				(key, window) -> (window == null || window.isExpired(now)) ? new Window(now.plus(WINDOW), 1)
						: new Window(window.end(), window.failures() + 1));
		if (windows.size() > PURGE_THRESHOLD) {
			windows.values().removeIf(window -> window.isExpired(now));
		}
	}

	public void reset(String clientKey) {
		windows.remove(clientKey);
	}

	private record Window(Instant end, int failures) {

		boolean isExpired(Instant now) {
			return !now.isBefore(end);
		}

	}

}
