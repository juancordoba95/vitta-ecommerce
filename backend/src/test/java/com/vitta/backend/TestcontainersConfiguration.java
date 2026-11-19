package com.vitta.backend;

import java.time.Duration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"))
				// Like Aiven, which enforces sql_require_primary_key.
				.withCommand("--sql-require-primary-key=ON")
				.withStartupTimeout(Duration.ofMinutes(3))
				// The JDBC readiness wait has its own limit (120 s by default); first-time init can be
				// slower on a loaded local Docker Desktop.
				.withConnectTimeoutSeconds(300);
	}

}
