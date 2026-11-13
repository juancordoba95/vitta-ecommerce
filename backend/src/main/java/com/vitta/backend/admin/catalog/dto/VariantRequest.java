package com.vitta.backend.admin.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VariantRequest(@NotBlank @Size(max = 10) String size,
		@NotNull @Min(0) @Max(VariantRequest.MAX_STOCK) Integer stock) {

	public static final int MAX_STOCK = 9999;

}
