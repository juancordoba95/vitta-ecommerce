package com.vitta.backend.cart.dto;

import java.math.BigDecimal;

/**
 * Validation result for one cart line. Product fields, {@code unitPrice} and
 * {@code availableStock} are {@code null} when the status is {@link CartItemStatus#NOT_FOUND}.
 */
public record CartItemValidation(Long variantId, String productSlug, String productName, String size,
		BigDecimal unitPrice, int quantity, Integer availableStock, CartItemStatus status) {
}
