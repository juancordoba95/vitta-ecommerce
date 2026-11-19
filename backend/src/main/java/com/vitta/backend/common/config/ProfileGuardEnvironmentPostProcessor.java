package com.vitta.backend.common.config;

import java.util.Set;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Fails startup when the {@code prod} and {@code dev} profiles are active together (for example
 * a stray {@code .env} with {@code SPRING_PROFILES_ACTIVE=dev}), before any bean is created, so
 * the dev seed and dev defaults can never reach a production database.
 */
public class ProfileGuardEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Set<String> active = Set.of(environment.getActiveProfiles());
		if (active.contains("prod") && active.contains("dev")) {
			throw new IllegalStateException(
					"The 'prod' and 'dev' profiles cannot be active together; check SPRING_PROFILES_ACTIVE");
		}
	}

	@Override
	public int getOrder() {
		// After ConfigDataEnvironmentPostProcessor, which resolves the active profiles.
		return Ordered.LOWEST_PRECEDENCE;
	}

}
