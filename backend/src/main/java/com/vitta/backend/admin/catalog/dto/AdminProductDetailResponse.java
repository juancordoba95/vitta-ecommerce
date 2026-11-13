package com.vitta.backend.admin.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import com.vitta.backend.catalog.dto.CategoryResponse;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductImage;
import com.vitta.backend.catalog.entity.ProductVariant;
import com.vitta.backend.catalog.entity.SizeOrder;

/**
 * Full product for the admin panel, including inactive products. Images are sorted by position
 * (the first is the cover) and variants by logical size order.
 */
public record AdminProductDetailResponse(Long id, String name, String slug, String description, BigDecimal price,
		String imageUrl, boolean active, CategoryResponse category, List<AdminImageResponse> images,
		List<AdminVariantResponse> variants, LocalDateTime createdAt, LocalDateTime updatedAt) {

	public static AdminProductDetailResponse from(Product product) {
		return new AdminProductDetailResponse(product.getId(), product.getName(), product.getSlug(),
				product.getDescription(), product.getPrice(), product.getImageUrl(), product.isActive(),
				CategoryResponse.from(product.getCategory()),
				product.getImages()
					.stream()
					.sorted(Comparator.comparingInt(ProductImage::getPosition))
					.map(AdminImageResponse::from)
					.toList(),
				product.getVariants()
					.stream()
					.sorted(Comparator.comparing(ProductVariant::getSize, SizeOrder.COMPARATOR))
					.map(AdminVariantResponse::from)
					.toList(),
				product.getCreatedAt(), product.getUpdatedAt());
	}

}
