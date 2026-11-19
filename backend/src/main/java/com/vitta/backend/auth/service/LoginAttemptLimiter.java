package com.vitta.backend.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.vitta.backend.auth.exception.TooManyLoginAttemptsException;
import org.springframework.stereotype.Component;

/**
 * In-memory, fixed-window limiter for failed logins, keyed by both account and client address.
 * <ul>
 * <li>Per email: after {@value #MAX_FAILURES_PER_EMAIL} failures within {@link #WINDOW}, that
 * account is locked until the window ends, whatever address the attempts come from.</li>
 * <li>Per IP: a higher cap of {@value #MAX_FAILURES_PER_IP} failures, as a backstop against one
 * client trying many emails. It is higher because in production the login is proxied by the
 * Next.js server, so many users share one address.</li>
 * </ul>
 * An attempt is rejected if either key reached its limit. State is per instance and lost on
 * restart, which is fine for a single-node deployment.
 */
@Component
public class LoginAttemptLimiter {

	public static final int MAX_FAILURES_PER_EMAIL = 5;

	public static final int MAX_FAILURES_PER_IP = 30;

	public static final Duration WINDOW = Duration.ofMinutes(1);

	/** Above this many tracked keys, expired windows are purged on each failure. */
	private static final int PURGE_THRESHOLD = 1_000;

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	private final Clock clock;

	public LoginAttemptLimiter(Clock clock) {
		this.clock = clock;
	}

	/**
	 * @param ip the client address
	 * @param email the normalized email of the attempt
	 * @throws TooManyLoginAttemptsException if the email or the address already reached its limit
	 */
	public void checkAllowed(String ip, String email) {
		Instant now = clock.instant();
		Duration retryAfter = max(blockedFor(emailKey(email), MAX_FAILURES_PER_EMAIL, now),
				blockedFor(ipKey(ip), MAX_FAILURES_PER_IP, now));
		if (!retryAfter.isZero()) {
			throw new TooManyLoginAttemptsException(retryAfter);
		}
	}

	public void recordFailure(String ip, String email) {
		Instant now = clock.instant();
		increment(emailKey(email), now);
		increment(ipKey(ip), now);
		if (windows.size() > PURGE_THRESHOLD) {
			windows.values().removeIf(window -> window.isExpired(now));
		}
	}

	/** Clears the failures of an account after a successful login; the IP window expires on its own. */
	public void reset(String email) {
		windows.remove(emailKey(email));
	}

	private Duration blockedFor(String key, int maxFailures, Instant now) {
		Window window = windows.get(key);
		if (window == null || window.isExpired(now) || window.failures() < maxFailures) {
			return Duration.ZERO;
		}
		return Duration.between(now, window.end());
	}

	private void increment(String key, Instant now) {
		windows.compute(key,
				(k, window) -> (window == null || window.isExpired(now)) ? new Window(now.plus(WINDOW), 1)
						: new Window(window.end(), window.failures() + 1));
	}

	private static Duration max(Duration a, Duration b) {
		return a.compareTo(b) >= 0 ? a : b;
	}

	private static String emailKey(String email) {
		return "email:" + email;
	}

	private static String ipKey(String ip) {
		return "ip:" + ip;
	}

	private record Window(Instant end, int failures) {

		boolean isExpired(Instant now) {
			return !now.isBefore(end);
		}

	}

}
