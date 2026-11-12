package com.vitta.backend.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.vitta.backend.auth.config.JwtConfig;
import com.vitta.backend.auth.config.JwtProperties;
import com.vitta.backend.auth.dto.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues HS256 access tokens for admins: {@code sub} is the email, {@code roles} is
 * {@code ["ADMIN"]}.
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

	public static final String ISSUER = "vitta-backend";

	public static final String ADMIN_ROLE = "ADMIN";

	private final JwtEncoder jwtEncoder;

	private final JwtProperties properties;

	private final Clock clock;

	public LoginResponse issueAdminToken(String email) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.expiration());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.subject(email)
			.issuedAt(now)
			.expiresAt(expiresAt)
			.claim(JwtConfig.ROLES_CLAIM, List.of(ADMIN_ROLE))
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new LoginResponse(token, expiresAt);
	}

}
