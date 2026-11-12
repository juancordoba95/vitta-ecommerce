package com.vitta.backend.auth;

import com.jayway.jsonpath.JsonPath;
import com.vitta.backend.TestcontainersConfiguration;
import com.vitta.backend.auth.entity.AdminUser;
import com.vitta.backend.auth.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end admin login against a real MySQL (Testcontainers): the {@code admin_users}
 * migration, the entity mapping and the full security chain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdminUserRepository adminUserRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void adminCanLogInAndAccessAdminEndpoints() throws Exception {
		adminUserRepository.saveAndFlush(new AdminUser("owner@vitta.test", passwordEncoder.encode("integration-pass")));

		String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{ "email": "owner@vitta.test", "password": "integration-pass" }
				"""))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String token = JsonPath.read(body, "$.token");

		mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("owner@vitta.test"));
	}

	@Test
	void adminEndpointsRequireToken() throws Exception {
		mockMvc.perform(get("/api/admin/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void healthIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

}
