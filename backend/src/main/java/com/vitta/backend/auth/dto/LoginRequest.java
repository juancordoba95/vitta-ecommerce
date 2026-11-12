package com.vitta.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login body. The password limit matches BCrypt, which only uses the first 72 bytes.
 */
public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(max = 72) String password) {
}
