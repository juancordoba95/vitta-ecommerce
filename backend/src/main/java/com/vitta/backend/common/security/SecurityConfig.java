package com.vitta.backend.common.security;

import java.util.List;

import com.vitta.backend.auth.config.JwtConfig;
import com.vitta.backend.common.config.CorsProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Stateless API security: public catalog/cart/login endpoints and local uploads, JWT-protected
 * {@code /api/admin/**} (role ADMIN), everything else denied.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
@Import(JwtConfig.class)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter,
			ProblemDetailSecurityErrorHandler errorHandler) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> {
			})
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR)
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/cart/validate", "/api/auth/login")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/uploads/**")
				.permitAll()
				.requestMatchers("/api/admin/**")
				.hasRole("ADMIN")
				.anyRequest()
				.denyAll())
			.oauth2ResourceServer(oauth2 -> oauth2
				.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
				.authenticationEntryPoint(errorHandler)
				.accessDeniedHandler(errorHandler))
			.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(errorHandler)
				.accessDeniedHandler(errorHandler));
		return http.build();
	}

	@Bean
	ProblemDetailSecurityErrorHandler problemDetailSecurityErrorHandler(
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		return new ProblemDetailSecurityErrorHandler(resolver);
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(corsProperties.allowedOrigin()));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", config);
		return source;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
