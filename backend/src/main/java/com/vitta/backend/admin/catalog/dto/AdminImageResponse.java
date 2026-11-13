package com.vitta.backend.admin.catalog.dto;

import com.vitta.backend.catalog.entity.ProductImage;

public record AdminImageResponse(Long id, String url, int position) {

	public static AdminImageResponse from(ProductImage image) {
		return new AdminImageResponse(image.getId(), image.getUrl(), image.getPosition());
	}

}
