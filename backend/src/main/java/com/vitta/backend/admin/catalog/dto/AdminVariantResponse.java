package com.vitta.backend.admin.catalog.dto;

import com.vitta.backend.catalog.entity.ProductVariant;

public record AdminVariantResponse(Long id, String size, int stock) {

	public static AdminVariantResponse from(ProductVariant variant) {
		return new AdminVariantResponse(variant.getId(), variant.getSize(), variant.getStock());
	}

}
