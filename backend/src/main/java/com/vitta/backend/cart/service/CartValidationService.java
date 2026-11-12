package com.vitta.backend.cart.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vitta.backend.cart.dto.CartItemRequest;
import com.vitta.backend.cart.dto.CartItemStatus;
import com.vitta.backend.cart.dto.CartItemValidation;
import com.vitta.backend.cart.dto.CartValidationRequest;
import com.vitta.backend.cart.dto.CartValidationResponse;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductVariant;
import com.vitta.backend.catalog.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks a client-side cart against current prices and stock. Read-only: it never reserves or
 * decrements stock.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartValidationService {

	private static final BigDecimal ZERO_TOTAL = BigDecimal.ZERO.setScale(2);

	private final ProductVariantRepository productVariantRepository;

	public CartValidationResponse validate(CartValidationRequest request) {
		List<Long> ids = request.items().stream().map(CartItemRequest::variantId).toList();
		Map<Long, ProductVariant> variantsById = productVariantRepository.findAllWithActiveProductByIdIn(ids)
			.stream()
			.collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

		List<CartItemValidation> items = request.items()
			.stream()
			.map((item) -> validateItem(item, variantsById.get(item.variantId())))
			.toList();
		BigDecimal total = items.stream()
			.filter((item) -> item.status() == CartItemStatus.OK)
			.map((item) -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
			.reduce(ZERO_TOTAL, BigDecimal::add);
		return new CartValidationResponse(items, total);
	}

	private CartItemValidation validateItem(CartItemRequest item, ProductVariant variant) {
		if (variant == null) {
			return new CartItemValidation(item.variantId(), null, null, null, null, item.quantity(), null,
					CartItemStatus.NOT_FOUND);
		}
		Product product = variant.getProduct();
		return new CartItemValidation(variant.getId(), product.getSlug(), product.getName(), variant.getSize(),
				product.getPrice(), item.quantity(), variant.getStock(),
				statusOf(variant.getStock(), item.quantity()));
	}

	static CartItemStatus statusOf(int stock, int quantity) {
		if (stock <= 0) {
			return CartItemStatus.OUT_OF_STOCK;
		}
		return (quantity > stock) ? CartItemStatus.INSUFFICIENT_STOCK : CartItemStatus.OK;
	}

}
