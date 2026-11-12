package com.vitta.backend.catalog.dto;

import com.vitta.backend.catalog.entity.ProductImage;

public record ProductImageResponse(String url, int position) {

	public static ProductImageResponse from(ProductImage image) {
		return new ProductImageResponse(image.getUrl(), image.getPosition());
	}

}
