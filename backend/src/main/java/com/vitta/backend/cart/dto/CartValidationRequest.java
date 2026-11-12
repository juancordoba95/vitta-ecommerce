package com.vitta.backend.cart.dto;

import java.util.List;
import java.util.Objects;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CartValidationRequest(@NotEmpty @Size(max = 50) List<@NotNull @Valid CartItemRequest> items) {

	/**
	 * Bean Validation property {@code variantIdsUnique}: each variant may appear at most once.
	 * Null items or ids are reported by their own constraints, not here.
	 */
	@AssertTrue(message = "must not contain duplicate variantId values")
	public boolean isVariantIdsUnique() {
		if (items == null) {
			return true;
		}
		List<Long> ids = items.stream()
			.filter(Objects::nonNull)
			.map(CartItemRequest::variantId)
			.filter(Objects::nonNull)
			.toList();
		return ids.stream().distinct().count() == ids.size();
	}

}
