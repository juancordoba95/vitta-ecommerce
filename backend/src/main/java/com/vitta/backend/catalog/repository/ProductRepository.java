package com.vitta.backend.catalog.repository;

import java.util.List;
import java.util.Optional;

import com.vitta.backend.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@EntityGraph(attributePaths = "category")
	List<Product> findAllByActiveTrueOrderByNameAsc();

	@EntityGraph(attributePaths = "category")
	List<Product> findAllByActiveTrueAndCategorySlugOrderByNameAsc(String categorySlug);

	@EntityGraph(attributePaths = { "category", "variants", "images" })
	Optional<Product> findBySlugAndActiveTrue(String slug);

	@EntityGraph(attributePaths = { "category", "variants", "images" })
	Optional<Product> findWithDetailsById(Long id);

	boolean existsBySlug(String slug);

	boolean existsBySlugAndIdNot(String slug, Long id);

	/**
	 * Admin search over all products (active or not). {@code namePattern} is a lowercase
	 * {@code LIKE} pattern escaped with {@code \}; {@code null} parameters mean no filter.
	 */
	@EntityGraph(attributePaths = "category")
	@Query(value = """
			select p from Product p
			where (:namePattern is null or lower(p.name) like :namePattern escape '\')
			  and (:categoryId is null or p.category.id = :categoryId)
			""", countQuery = """
			select count(p) from Product p
			where (:namePattern is null or lower(p.name) like :namePattern escape '\')
			  and (:categoryId is null or p.category.id = :categoryId)
			""")
	Page<Product> searchAdmin(@Param("namePattern") String namePattern, @Param("categoryId") Long categoryId,
			Pageable pageable);

}
