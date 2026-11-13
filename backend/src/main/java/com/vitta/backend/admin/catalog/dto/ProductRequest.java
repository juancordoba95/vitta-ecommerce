package com.vitta.backend.admin.catalog.dto;

import java.math.BigDecimal;

import com.vitta.backend.catalog.service.SlugGenerator;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Create/update body. An empty or missing {@code slug} is generated from the name on create and
 * left unchanged on update.
 */
public record ProductRequest(@NotBlank @Size(max = 150) String name, @Size(max = 2000) String description,
		@NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal price,
		@NotNull @Positive Long categoryId,
		@Size(max = 170) @Pattern(regexp = "^$|" + SlugGenerator.PATTERN,
				message = SlugGenerator.PATTERN_MESSAGE) String slug) {
}
