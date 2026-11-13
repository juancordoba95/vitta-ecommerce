package com.vitta.backend.catalog.repository;

import java.util.Optional;

import com.vitta.backend.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

	/** The image with its product and the product's whole gallery, to renumber after a delete. */
	@EntityGraph(attributePaths = { "product", "product.images" })
	Optional<ProductImage> findWithProductById(Long id);

}
