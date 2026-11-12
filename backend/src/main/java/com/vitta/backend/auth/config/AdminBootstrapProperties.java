package com.vitta.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Initial admin account, bound from {@code ADMIN_EMAIL} and {@code ADMIN_PASSWORD}. Both are
 * optional: when either is missing no admin is created.
 */
@ConfigurationProperties("app.admin")
public record AdminBootstrapProperties(String email, String password) {

	public boolean isConfigured() {
		return StringUtils.hasText(email) && StringUtils.hasText(password);
	}

}
