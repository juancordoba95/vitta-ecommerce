package com.vitta.backend.catalog.service;

import java.util.List;

import com.vitta.backend.catalog.dto.ProductDetailResponse;
import com.vitta.backend.catalog.dto.ProductSummaryResponse;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.repository.CategoryRepository;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;

	private final CategoryRepository categoryRepository;

	/**
	 * Lists active products, optionally filtered by category slug ({@code null} means no filter).
	 * @throws ResourceNotFoundException if a category slug is given and does not exist
	 */
	public List<ProductSummaryResponse> findActive(String categorySlug) {
		List<Product> products;
		if (categorySlug == null) {
			products = productRepository.findAllByActiveTrueOrderByNameAsc();
		}
		else {
			if (!categoryRepository.existsBySlug(categorySlug)) {
				throw new ResourceNotFoundException("Category", categorySlug);
			}
			products = productRepository.findAllByActiveTrueAndCategorySlugOrderByNameAsc(categorySlug);
		}
		return products.stream().map(ProductSummaryResponse::from).toList();
	}

	/**
	 * Returns an active product with its category and variants.
	 * @throws ResourceNotFoundException if no active product has the given slug
	 */
	public ProductDetailResponse findActiveBySlug(String slug) {
		return productRepository.findBySlugAndActiveTrue(slug)
			.map(ProductDetailResponse::from)
			.orElseThrow(() -> new ResourceNotFoundException("Product", slug));
	}

}
