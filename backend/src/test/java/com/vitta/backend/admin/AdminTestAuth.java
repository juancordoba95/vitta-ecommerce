package com.vitta.backend.admin;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** MockMvc request post-processors for authenticated admin calls. */
public final class AdminTestAuth {

	private AdminTestAuth() {
	}

	public static JwtRequestPostProcessor admin() {
		return jwt().jwt(token -> token.subject("admin@vitta.test"))
			.authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
	}

	public static JwtRequestPostProcessor nonAdmin() {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
	}

}
