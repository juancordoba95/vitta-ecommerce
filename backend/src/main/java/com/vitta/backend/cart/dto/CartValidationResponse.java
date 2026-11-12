package com.vitta.backend.cart.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param items one result per requested item, in request order
 * @param total sum of {@code unitPrice * quantity} over the {@link CartItemStatus#OK} items only
 */
public record CartValidationResponse(List<CartItemValidation> items, BigDecimal total) {
}
