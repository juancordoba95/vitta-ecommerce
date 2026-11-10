package com.vitta.backend.catalog.dto;

import java.math.BigDecimal;

import com.vitta.backend.catalog.entity.Product;

public record ProductSummaryResponse(Long id, String name, String slug, BigDecimal price, String imageUrl,
		String categorySlug) {

	public static ProductSummaryResponse from(Product product) {
		return new ProductSummaryResponse(product.getId(), product.getName(), product.getSlug(), product.getPrice(),
				product.getImageUrl(), product.getCategory().getSlug());
	}

}
