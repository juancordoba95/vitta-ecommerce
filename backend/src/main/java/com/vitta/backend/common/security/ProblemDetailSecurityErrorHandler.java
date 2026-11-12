package com.vitta.backend.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Hands security failures raised in the filter chain (401/403) to Spring MVC's exception
 * resolvers, so {@code GlobalExceptionHandler} renders them as the same Problem Details as
 * every other API error.
 */
public class ProblemDetailSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final HandlerExceptionResolver resolver;

	public ProblemDetailSecurityErrorHandler(HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		resolver.resolveException(request, response, null, authException);
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) {
		resolver.resolveException(request, response, null, accessDeniedException);
	}

}
