package com.vitta.backend.auth.repository;

import java.util.Optional;

import com.vitta.backend.auth.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

	Optional<AdminUser> findByEmail(String email);

	boolean existsByEmail(String email);

}
