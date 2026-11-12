package com.vitta.backend.auth.config;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class JwtPropertiesTest {

	private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";

	@Test
	void acceptsSecretOfAtLeast32Bytes() {
		JwtProperties properties = new JwtProperties(VALID_SECRET, Duration.ofHours(8));

		assertThat(properties.secret()).isEqualTo(VALID_SECRET);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "too-short", "0123456789abcdef0123456789abcde" })
	void rejectsMissingOrShortSecret(String secret) {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(secret, Duration.ofHours(8)))
			.withMessageContaining("JWT_SECRET");
	}

	@Test
	void rejectsNonPositiveExpiration() {
		assertThatIllegalArgumentException().isThrownBy(() -> new JwtProperties(VALID_SECRET, Duration.ZERO))
			.withMessageContaining("JWT_EXPIRATION");
	}

}
