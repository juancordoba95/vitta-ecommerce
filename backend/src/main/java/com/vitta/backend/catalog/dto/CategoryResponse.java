package com.vitta.backend.catalog.dto;

import com.vitta.backend.catalog.entity.Category;

public record CategoryResponse(Long id, String name, String slug) {

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
	}

}
