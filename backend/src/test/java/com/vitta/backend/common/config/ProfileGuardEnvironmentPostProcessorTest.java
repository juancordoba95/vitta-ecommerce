package com.vitta.backend.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class ProfileGuardEnvironmentPostProcessorTest {

	private final ProfileGuardEnvironmentPostProcessor guard = new ProfileGuardEnvironmentPostProcessor();

	@Test
	void rejectsProdAndDevTogether() {
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod", "dev");

		assertThatIllegalStateException()
			.isThrownBy(() -> guard.postProcessEnvironment(environment, new SpringApplication()))
			.withMessageContaining("SPRING_PROFILES_ACTIVE");
	}

	@Test
	void allowsASingleProfile() {
		for (String profile : new String[] { "prod", "dev" }) {
			MockEnvironment environment = new MockEnvironment();
			environment.setActiveProfiles(profile);

			assertThatCode(() -> guard.postProcessEnvironment(environment, new SpringApplication()))
				.doesNotThrowAnyException();
		}
	}

}
