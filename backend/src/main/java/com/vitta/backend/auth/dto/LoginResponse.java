package com.vitta.backend.auth.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt) {
}
