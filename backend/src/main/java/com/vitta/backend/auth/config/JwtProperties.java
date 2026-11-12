package com.vitta.backend.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT settings, bound from {@code JWT_SECRET} and {@code JWT_EXPIRATION}. The secret signs
 * tokens with HS256, so it must be at least 256 bits (32 bytes); a shorter or missing secret
 * fails startup.
 */
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, @DefaultValue("8h") Duration expiration) {

	public static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException(
					"app.security.jwt.secret (JWT_SECRET) must be at least %d bytes".formatted(MIN_SECRET_BYTES));
		}
		if (expiration == null || expiration.isNegative() || expiration.isZero()) {
			throw new IllegalArgumentException("app.security.jwt.expiration (JWT_EXPIRATION) must be positive");
		}
	}

}
