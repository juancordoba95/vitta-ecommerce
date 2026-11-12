package com.vitta.backend.auth.controller;

import com.vitta.backend.auth.dto.AdminMeResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	/** Lets the frontend check that its token is still valid. */
	@GetMapping("/me")
	public AdminMeResponse me(@AuthenticationPrincipal Jwt jwt) {
		return new AdminMeResponse(jwt.getSubject());
	}

}
