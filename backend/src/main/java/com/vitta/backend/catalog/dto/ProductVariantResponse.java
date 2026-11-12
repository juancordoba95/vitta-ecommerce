package com.vitta.backend.catalog.dto;

import com.vitta.backend.catalog.entity.ProductVariant;

public record ProductVariantResponse(Long id, String size, int stock, boolean available) {

	public static ProductVariantResponse from(ProductVariant variant) {
		return new ProductVariantResponse(variant.getId(), variant.getSize(), variant.getStock(),
				variant.getStock() > 0);
	}

}
