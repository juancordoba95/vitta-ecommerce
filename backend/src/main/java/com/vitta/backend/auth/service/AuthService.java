package com.vitta.backend.auth.service;

import java.util.Optional;
import java.util.UUID;

import com.vitta.backend.auth.dto.LoginRequest;
import com.vitta.backend.auth.dto.LoginResponse;
import com.vitta.backend.auth.entity.AdminUser;
import com.vitta.backend.auth.exception.InvalidCredentialsException;
import com.vitta.backend.auth.repository.AdminUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin login. Unknown emails and wrong passwords produce the same error and both run a
 * BCrypt comparison, so neither the response nor its timing reveals which accounts exist.
 */
@Service
public class AuthService {

	private final AdminUserRepository adminUserRepository;

	private final PasswordEncoder passwordEncoder;

	private final JwtTokenService jwtTokenService;

	private final LoginAttemptLimiter loginAttemptLimiter;

	/** Compared against when the email is unknown, to keep timing uniform. */
	private final String dummyHash;

	public AuthService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService, LoginAttemptLimiter loginAttemptLimiter) {
		this.adminUserRepository = adminUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.loginAttemptLimiter = loginAttemptLimiter;
		this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request, String clientIp) {
		String email = EmailNormalizer.normalize(request.email());
		loginAttemptLimiter.checkAllowed(clientIp, email);

		Optional<AdminUser> admin = adminUserRepository.findByEmail(email);
		String hash = admin.map(AdminUser::getPasswordHash).orElse(dummyHash);
		boolean passwordMatches = matches(request.password(), hash);

		if (admin.isEmpty() || !passwordMatches) {
			loginAttemptLimiter.recordFailure(clientIp, email);
			throw new InvalidCredentialsException();
		}
		loginAttemptLimiter.reset(email);
		return jwtTokenService.issueAdminToken(admin.get().getEmail());
	}

	private boolean matches(String rawPassword, String hash) {
		try {
			return passwordEncoder.matches(rawPassword, hash);
		}
		catch (IllegalArgumentException ex) {
			// For example a password over BCrypt's 72-byte limit (multi-byte characters).
			return false;
		}
	}

}
