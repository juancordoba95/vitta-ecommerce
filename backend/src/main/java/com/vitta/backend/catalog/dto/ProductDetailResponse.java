package com.vitta.backend.catalog.dto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductImage;
import com.vitta.backend.catalog.entity.ProductVariant;
import com.vitta.backend.catalog.entity.SizeOrder;

/**
 * Product detail. {@code imageUrl} is the cover (same as in listings); {@code images} is the
 * gallery sorted by position and {@code variants} are sorted by logical size order.
 */
public record ProductDetailResponse(Long id, String name, String slug, String description, BigDecimal price,
		String imageUrl, CategoryResponse category, List<ProductImageResponse> images,
		List<ProductVariantResponse> variants) {

	public static ProductDetailResponse from(Product product) {
		return new ProductDetailResponse(product.getId(), product.getName(), product.getSlug(),
				product.getDescription(), product.getPrice(), product.getImageUrl(),
				CategoryResponse.from(product.getCategory()),
				product.getImages()
					.stream()
					.sorted(Comparator.comparingInt(ProductImage::getPosition))
					.map(ProductImageResponse::from)
					.toList(),
				product.getVariants()
					.stream()
					.sorted(Comparator.comparing(ProductVariant::getSize, SizeOrder.COMPARATOR))
					.map(ProductVariantResponse::from)
					.toList());
	}

}
