package com.vitta.backend.auth.controller;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.spec.SecretKeySpec;

import com.jayway.jsonpath.JsonPath;
import com.vitta.backend.auth.entity.AdminUser;
import com.vitta.backend.auth.repository.AdminUserRepository;
import com.vitta.backend.auth.service.AuthService;
import com.vitta.backend.auth.service.JwtTokenService;
import com.vitta.backend.auth.service.LoginAttemptLimiter;
import com.vitta.backend.catalog.controller.CategoryController;
import com.vitta.backend.catalog.service.CategoryService;
import com.vitta.backend.common.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login, token validation and access rules through the real security filter chain (no
 * database: the admin repository is mocked).
 */
@WebMvcTest({ AuthController.class, AdminController.class, CategoryController.class })
@Import({ SecurityConfig.class, AuthService.class, JwtTokenService.class, LoginAttemptLimiter.class })
class AuthControllerTest {

	private static final String EMAIL = "admin@vitta.test";

	private static final String PASSWORD = "correct-horse-battery";

	/** Each test logs in from its own address so the shared rate limiter never leaks state. */
	private static final AtomicInteger NEXT_IP = new AtomicInteger(1);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	@MockitoSpyBean
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private AdminUserRepository adminUserRepository;

	@MockitoBean
	private CategoryService categoryService;

	private String clientIp;

	@BeforeEach
	void setUp() {
		clientIp = "10.0.0." + NEXT_IP.getAndIncrement();
		AdminUser admin = new AdminUser(EMAIL, passwordEncoder.encode(PASSWORD));
		given(adminUserRepository.findByEmail(anyString())).willReturn(Optional.empty());
		given(adminUserRepository.findByEmail(EMAIL)).willReturn(Optional.of(admin));
		clearInvocations(passwordEncoder);
	}

	@Test
	void loginReturnsTokenThatGrantsAdminAccess() throws Exception {
		String body = login(EMAIL, PASSWORD).andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.token").isString())
			.andExpect(jsonPath("$.expiresAt").isString())
			.andReturn()
			.getResponse()
			.getContentAsString();
		String token = JsonPath.read(body, "$.token");

		mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(EMAIL));
	}

	@Test
	void loginIgnoresEmailCase() throws Exception {
		login("Admin@Vitta.TEST", PASSWORD).andExpect(status().isOk());
	}

	@Test
	void wrongPasswordAndUnknownUserGetTheSameGenericUnauthorized() throws Exception {
		String wrongPassword = login(EMAIL, "wrong-password").andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Invalid credentials"))
			.andExpect(jsonPath("$.detail").value("Invalid email or password"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		then(passwordEncoder).should().matches(eq("wrong-password"), anyString());

		String unknownUser = login("nobody@vitta.test", "whatever").andExpect(status().isUnauthorized())
			.andReturn()
			.getResponse()
			.getContentAsString();
		// BCrypt also runs for unknown emails, so timing does not reveal which accounts exist.
		then(passwordEncoder).should().matches(eq("whatever"), anyString());

		assertThat(unknownUser).isEqualTo(wrongPassword);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", //
			"{ \"email\": \"admin@vitta.test\" }", //
			"{ \"password\": \"secret\" }", //
			"{ \"email\": \"not-an-email\", \"password\": \"secret\" }", //
			"{ \"email\": \"admin@vitta.test\", \"password\": \"  \" }", //
			"not json" })
	void returnsBadRequestForInvalidBody(String body) throws Exception {
		mockMvc.perform(post("/api/auth/login").with(fromIp(clientIp))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void blocksLoginAfterTooManyFailures() throws Exception {
		for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
			login(EMAIL, "wrong-password").andExpect(status().isUnauthorized());
		}

		// Even the right password is rejected until the window ends.
		login(EMAIL, PASSWORD).andExpect(status().isTooManyRequests())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(429))
			.andExpect(jsonPath("$.title").value("Too many login attempts"))
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER));

		// Other clients are not affected.
		clientIp = "10.0.1." + NEXT_IP.getAndIncrement();
		login(EMAIL, PASSWORD).andExpect(status().isOk());
	}

	@Test
	void adminMeWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/admin/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Unauthorized"))
			.andExpect(jsonPath("$.instance").value("/api/admin/me"));
	}

	@Test
	void adminMeWithMalformedTokenIsUnauthorized() throws Exception {
		expectUnauthorized("not-a-jwt");
	}

	@Test
	void adminMeWithTokenSignedByAnotherKeyIsUnauthorized() throws Exception {
		JwtEncoder otherEncoder = NimbusJwtEncoder
			.withSecretKey(new SecretKeySpec("another-secret-that-is-at-least-32-bytes-long".getBytes(
					StandardCharsets.UTF_8), "HmacSHA256"))
			.algorithm(MacAlgorithm.HS256)
			.build();
		expectUnauthorized(encode(otherEncoder, Instant.now().plusSeconds(3600), List.of("ADMIN")));
	}

	@Test
	void adminMeWithExpiredTokenIsUnauthorized() throws Exception {
		expectUnauthorized(encode(jwtEncoder, Instant.now().minusSeconds(3600), List.of("ADMIN")));
	}

	@Test
	void adminMeWithoutAdminRoleIsForbidden() throws Exception {
		String token = encode(jwtEncoder, Instant.now().plusSeconds(3600), List.of("CUSTOMER"));
		mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@Test
	void publicEndpointsDoNotNeedToken() throws Exception {
		given(categoryService.findAll()).willReturn(List.of());
		mockMvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void unlistedRoutesAreDenied() throws Exception {
		mockMvc.perform(get("/api/unknown"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void corsPreflightAllowsAuthorizationHeaderAndWriteMethods() throws Exception {
		mockMvc.perform(options("/api/admin/me").header(HttpHeaders.ORIGIN, "http://localhost:3000")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "DELETE")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login").with(fromIp(clientIp))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"email\": \"%s\", \"password\": \"%s\" }".formatted(email, password)));
	}

	private void expectUnauthorized(String token) throws Exception {
		mockMvc.perform(get("/api/admin/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401));
	}

	private static String encode(JwtEncoder encoder, Instant expiresAt, List<String> roles) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.subject(EMAIL)
			.issuedAt(expiresAt.minusSeconds(7200))
			.expiresAt(expiresAt)
			.claim("roles", roles)
			.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	private static RequestPostProcessor fromIp(String ip) {
		return request -> {
			request.setRemoteAddr(ip);
			return request;
		};
	}

}
