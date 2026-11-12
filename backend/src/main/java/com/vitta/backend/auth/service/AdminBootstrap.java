package com.vitta.backend.auth.service;

import com.vitta.backend.auth.config.AdminBootstrapProperties;
import com.vitta.backend.auth.entity.AdminUser;
import com.vitta.backend.auth.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the admin from {@code ADMIN_EMAIL}/{@code ADMIN_PASSWORD} on startup if it does not
 * exist yet. Existing admins are never modified. Missing variables only log a warning.
 */
@Component
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

	private final AdminBootstrapProperties properties;

	private final AdminUserRepository adminUserRepository;

	private final PasswordEncoder passwordEncoder;

	public AdminBootstrap(AdminBootstrapProperties properties, AdminUserRepository adminUserRepository,
			PasswordEncoder passwordEncoder) {
		this.properties = properties;
		this.adminUserRepository = adminUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!properties.isConfigured()) {
			log.warn("ADMIN_EMAIL and/or ADMIN_PASSWORD are not set: no admin user will be created");
			return;
		}
		String email = EmailNormalizer.normalize(properties.email());
		if (adminUserRepository.existsByEmail(email)) {
			log.debug("Admin user already exists, skipping bootstrap");
			return;
		}
		adminUserRepository.save(new AdminUser(email, passwordEncoder.encode(properties.password())));
		log.info("Created admin user from ADMIN_EMAIL");
	}

}
