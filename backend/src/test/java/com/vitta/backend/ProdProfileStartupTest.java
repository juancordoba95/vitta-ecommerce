package com.vitta.backend;

import com.vitta.backend.storage.StorageConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the real application*.yaml files with the prod profile (no database needed: only the
 * storage configuration is registered) and checks the production safeguards.
 */
class ProdProfileStartupTest {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withInitializer(new ConfigDataApplicationContextInitializer())
		.withUserConfiguration(StorageConfig.class)
		// Explicit values win over a developer's local backend/.env.
		.withPropertyValues("spring.profiles.active=prod", "FRONTEND_URL=https://vitta.example");

	@Test
	void failsToStartWithoutCloudinaryUrl() {
		runner.withPropertyValues("app.storage.cloudinary-url=").run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("CLOUDINARY_URL");
		});
	}

	@Test
	void startsWithCloudinaryUrlAndAppliesProductionSettings() {
		runner.withPropertyValues("app.storage.cloudinary-url=cloudinary://000000000000000:fake-secret@fake-cloud")
			.run(context -> {
				assertThat(context).hasNotFailed();
				var env = context.getEnvironment();
				assertThat(env.getProperty("spring.flyway.locations")).isEqualTo("classpath:db/migration");
				assertThat(env.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo("5");
				assertThat(env.getProperty("server.forward-headers-strategy")).isEqualTo("native");
			});
	}

}
