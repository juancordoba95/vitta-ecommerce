package com.vitta.backend.admin.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.vitta.backend.catalog.dto.CategoryResponse;
import com.vitta.backend.catalog.entity.Product;

public record AdminProductSummaryResponse(Long id, String name, String slug, BigDecimal price, String imageUrl,
		CategoryResponse category, boolean active, LocalDateTime updatedAt) {

	public static AdminProductSummaryResponse from(Product product) {
		return new AdminProductSummaryResponse(product.getId(), product.getName(), product.getSlug(),
				product.getPrice(), product.getImageUrl(), CategoryResponse.from(product.getCategory()),
				product.isActive(), product.getUpdatedAt());
	}

}
