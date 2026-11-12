package com.vitta.backend.auth.service;

import com.vitta.backend.auth.config.AdminBootstrapProperties;
import com.vitta.backend.auth.entity.AdminUser;
import com.vitta.backend.auth.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

	@Mock
	private AdminUserRepository adminUserRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	void createsAdminWithHashedPasswordWhenMissing() {
		given(adminUserRepository.existsByEmail("admin@vitta.test")).willReturn(false);
		given(passwordEncoder.encode("s3cret-password")).willReturn("hashed");

		bootstrap(" Admin@Vitta.test ", "s3cret-password").run(null);

		ArgumentCaptor<AdminUser> saved = ArgumentCaptor.forClass(AdminUser.class);
		then(adminUserRepository).should().save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("admin@vitta.test");
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
	}

	@Test
	void doesNothingWhenAdminAlreadyExists() {
		given(adminUserRepository.existsByEmail("admin@vitta.test")).willReturn(true);

		bootstrap("admin@vitta.test", "s3cret-password").run(null);

		then(adminUserRepository).should(never()).save(any());
		then(passwordEncoder).shouldHaveNoInteractions();
	}

	@Test
	void doesNothingWhenVariablesAreMissing() {
		bootstrap(null, "s3cret-password").run(null);
		bootstrap("admin@vitta.test", "").run(null);

		then(adminUserRepository).shouldHaveNoInteractions();
		then(passwordEncoder).shouldHaveNoInteractions();
	}

	private AdminBootstrap bootstrap(String email, String password) {
		return new AdminBootstrap(new AdminBootstrapProperties(email, password), adminUserRepository,
				passwordEncoder);
	}

}
