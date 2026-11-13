package com.vitta.backend.admin.catalog.dto;

import com.vitta.backend.catalog.service.SlugGenerator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create/update body. Same slug rules as products: generated on create when empty, kept on
 * update when empty.
 */
public record CategoryRequest(@NotBlank @Size(max = 100) String name,
		@Size(max = 120) @Pattern(regexp = "^$|" + SlugGenerator.PATTERN,
				message = SlugGenerator.PATTERN_MESSAGE) String slug) {
}
