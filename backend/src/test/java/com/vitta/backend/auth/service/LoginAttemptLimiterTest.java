package com.vitta.backend.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.vitta.backend.auth.exception.TooManyLoginAttemptsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {

	private static final String CLIENT = "10.0.0.1";

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-12T10:00:00Z"));

	private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

	@Test
	void allowsUpToMaxFailures() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES - 1);

		assertThatCode(() -> limiter.checkAllowed(CLIENT)).doesNotThrowAnyException();
	}

	@Test
	void blocksOnceMaxFailuresAreReachedWithinTheWindow() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES);
		clock.advance(Duration.ofSeconds(20));

		assertThatThrownBy(() -> limiter.checkAllowed(CLIENT)).isInstanceOf(TooManyLoginAttemptsException.class)
			.satisfies(ex -> assertThat(((TooManyLoginAttemptsException) ex).getRetryAfter())
				.isEqualTo(Duration.ofSeconds(40)));
	}

	@Test
	void unblocksWhenTheWindowEnds() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES);
		clock.advance(LoginAttemptLimiter.WINDOW);

		assertThatCode(() -> limiter.checkAllowed(CLIENT)).doesNotThrowAnyException();
	}

	@Test
	void failuresInAnExpiredWindowStartANewCount() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES - 1);
		clock.advance(LoginAttemptLimiter.WINDOW);
		failTimes(1);

		assertThatCode(() -> limiter.checkAllowed(CLIENT)).doesNotThrowAnyException();
	}

	@Test
	void resetClearsFailures() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES);
		limiter.reset(CLIENT);

		assertThatCode(() -> limiter.checkAllowed(CLIENT)).doesNotThrowAnyException();
	}

	@Test
	void tracksClientsIndependently() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES);

		assertThatCode(() -> limiter.checkAllowed("10.0.0.2")).doesNotThrowAnyException();
	}

	private void failTimes(int times) {
		for (int i = 0; i < times; i++) {
			limiter.recordFailure(CLIENT);
		}
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

	}

}
