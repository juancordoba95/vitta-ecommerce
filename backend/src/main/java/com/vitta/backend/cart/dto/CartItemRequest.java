package com.vitta.backend.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemRequest(@NotNull @Positive Long variantId, @NotNull @Min(1) @Max(999) Integer quantity) {
}
