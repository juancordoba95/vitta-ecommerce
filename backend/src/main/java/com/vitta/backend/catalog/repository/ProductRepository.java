package com.vitta.backend.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.vitta.backend.catalog.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@EntityGraph(attributePaths = "category")
	List<Product> findAllByActiveTrueOrderByNameAsc();

	@EntityGraph(attributePaths = "category")
	List<Product> findAllByActiveTrueAndCategorySlugOrderByNameAsc(String categorySlug);

	@EntityGraph(attributePaths = { "category", "variants", "images" })
	Optional<Product> findBySlugAndActiveTrue(String slug);

}
