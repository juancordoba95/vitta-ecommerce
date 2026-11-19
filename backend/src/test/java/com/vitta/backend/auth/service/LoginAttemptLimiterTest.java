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

	private static final String IP = "10.0.0.1";

	private static final String EMAIL = "admin@vitta.test";

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-12T10:00:00Z"));

	private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

	@Test
	void allowsUpToMaxFailuresPerEmail() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL - 1);

		assertThatCode(() -> limiter.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();
	}

	@Test
	void blocksTheEmailOnceMaxFailuresAreReachedWithinTheWindow() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL);
		clock.advance(Duration.ofSeconds(20));

		assertThatThrownBy(() -> limiter.checkAllowed(IP, EMAIL)).isInstanceOf(TooManyLoginAttemptsException.class)
			.satisfies(ex -> assertThat(((TooManyLoginAttemptsException) ex).getRetryAfter())
				.isEqualTo(Duration.ofSeconds(40)));
	}

	@Test
	void blocksTheEmailFromAnyAddress() {
		for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL; i++) {
			limiter.recordFailure("10.0.0." + i, EMAIL);
		}

		assertThatThrownBy(() -> limiter.checkAllowed("10.9.9.9", EMAIL))
			.isInstanceOf(TooManyLoginAttemptsException.class);
	}

	@Test
	void otherEmailsFromTheSameAddressAreNotBlockedByOneAccount() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL);

		assertThatCode(() -> limiter.checkAllowed(IP, "other@vitta.test")).doesNotThrowAnyException();
	}

	@Test
	void blocksTheAddressAfterMaxFailuresPerIpAcrossEmails() {
		for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES_PER_IP - 1; i++) {
			limiter.recordFailure(IP, "user" + i + "@vitta.test");
		}
		assertThatCode(() -> limiter.checkAllowed(IP, "fresh@vitta.test")).doesNotThrowAnyException();

		limiter.recordFailure(IP, "last@vitta.test");

		assertThatThrownBy(() -> limiter.checkAllowed(IP, "fresh@vitta.test"))
			.isInstanceOf(TooManyLoginAttemptsException.class);
		assertThatCode(() -> limiter.checkAllowed("10.0.0.2", "fresh@vitta.test")).doesNotThrowAnyException();
	}

	@Test
	void unblocksWhenTheWindowEnds() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL);
		clock.advance(LoginAttemptLimiter.WINDOW);

		assertThatCode(() -> limiter.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();
	}

	@Test
	void failuresInAnExpiredWindowStartANewCount() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL - 1);
		clock.advance(LoginAttemptLimiter.WINDOW);
		failTimes(1);

		assertThatCode(() -> limiter.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();
	}

	@Test
	void resetClearsTheEmailFailures() {
		failTimes(LoginAttemptLimiter.MAX_FAILURES_PER_EMAIL);
		limiter.reset(EMAIL);

		assertThatCode(() -> limiter.checkAllowed(IP, EMAIL)).doesNotThrowAnyException();
	}

	private void failTimes(int times) {
		for (int i = 0; i < times; i++) {
			limiter.recordFailure(IP, EMAIL);
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
