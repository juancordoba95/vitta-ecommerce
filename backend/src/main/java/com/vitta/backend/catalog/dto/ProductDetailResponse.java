package com.vitta.backend.catalog.dto;

import java.math.BigDecimal;
import java.util.List;

import com.vitta.backend.catalog.entity.Product;

public record ProductDetailResponse(Long id, String name, String slug, String description, BigDecimal price,
		String imageUrl, CategoryResponse category, List<ProductVariantResponse> variants) {

	public static ProductDetailResponse from(Product product) {
		return new ProductDetailResponse(product.getId(), product.getName(), product.getSlug(),
				product.getDescription(), product.getPrice(), product.getImageUrl(),
				CategoryResponse.from(product.getCategory()),
				product.getVariants().stream().map(ProductVariantResponse::from).toList());
	}

}
