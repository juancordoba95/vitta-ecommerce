package com.vitta.backend.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * CORS settings. {@code allowedOrigin} is bound from {@code FRONTEND_URL}.
 */
@Validated
@ConfigurationProperties("app.cors")
public record CorsProperties(@NotBlank String allowedOrigin) {
}
