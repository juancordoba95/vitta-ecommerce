package com.vitta.backend.catalog.dto;

import com.vitta.backend.catalog.entity.ProductVariant;

public record ProductVariantResponse(String size, int stock) {

	public static ProductVariantResponse from(ProductVariant variant) {
		return new ProductVariantResponse(variant.getSize(), variant.getStock());
	}

}
