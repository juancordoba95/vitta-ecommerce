package com.vitta.backend.catalog.repository;

import java.util.List;

import com.vitta.backend.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	List<Category> findAllByOrderByNameAsc();

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);

}
