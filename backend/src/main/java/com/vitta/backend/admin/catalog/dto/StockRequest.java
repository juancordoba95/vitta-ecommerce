package com.vitta.backend.admin.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockRequest(@NotNull @Min(0) @Max(VariantRequest.MAX_STOCK) Integer stock) {
}
