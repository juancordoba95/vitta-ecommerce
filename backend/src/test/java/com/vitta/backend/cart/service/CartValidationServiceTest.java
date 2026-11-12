package com.vitta.backend.cart.service;

import java.math.BigDecimal;
import java.util.List;

import com.vitta.backend.cart.dto.CartItemRequest;
import com.vitta.backend.cart.dto.CartItemStatus;
import com.vitta.backend.cart.dto.CartItemValidation;
import com.vitta.backend.cart.dto.CartValidationRequest;
import com.vitta.backend.cart.dto.CartValidationResponse;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductVariant;
import com.vitta.backend.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CartValidationServiceTest {

	private final Product encaje = new Product("Conjunto Encaje", "conjunto-encaje", null, new BigDecimal("42900.00"),
			new Category("Conjuntos", "conjuntos"), null, true);

	@Mock
	private ProductVariantRepository productVariantRepository;

	@InjectMocks
	private CartValidationService cartValidationService;

	@Test
	void okWhenStockCoversQuantity() {
		givenVariants(variant(1L, "S", 4));

		CartValidationResponse result = validate(item(1L, 4));

		assertThat(result.items()).singleElement().satisfies((item) -> {
			assertThat(item.variantId()).isEqualTo(1L);
			assertThat(item.productSlug()).isEqualTo("conjunto-encaje");
			assertThat(item.productName()).isEqualTo("Conjunto Encaje");
			assertThat(item.size()).isEqualTo("S");
			assertThat(item.unitPrice()).isEqualByComparingTo("42900.00");
			assertThat(item.quantity()).isEqualTo(4);
			assertThat(item.availableStock()).isEqualTo(4);
			assertThat(item.status()).isEqualTo(CartItemStatus.OK);
		});
		assertThat(result.total()).isEqualTo(new BigDecimal("171600.00"));
	}

	@Test
	void insufficientStockWhenQuantityExceedsStock() {
		givenVariants(variant(1L, "M", 2));

		CartValidationResponse result = validate(item(1L, 3));

		assertThat(result.items()).extracting(CartItemValidation::status, CartItemValidation::availableStock)
			.containsExactly(tuple(CartItemStatus.INSUFFICIENT_STOCK, 2));
		assertThat(result.total()).isEqualByComparingTo("0");
	}

	@Test
	void outOfStockWhenVariantHasNoStock() {
		givenVariants(variant(1L, "L", 0));

		CartValidationResponse result = validate(item(1L, 1));

		assertThat(result.items()).extracting(CartItemValidation::status, CartItemValidation::availableStock)
			.containsExactly(tuple(CartItemStatus.OUT_OF_STOCK, 0));
		assertThat(result.total()).isEqualByComparingTo("0");
	}

	@Test
	void notFoundWhenVariantIsUnknownOrItsProductIsInactive() {
		// The repository leaves out unknown ids and variants of inactive products alike.
		givenVariants();

		CartValidationResponse result = validate(item(99L, 2));

		assertThat(result.items()).singleElement().satisfies((item) -> {
			assertThat(item.variantId()).isEqualTo(99L);
			assertThat(item.quantity()).isEqualTo(2);
			assertThat(item.status()).isEqualTo(CartItemStatus.NOT_FOUND);
			assertThat(item.productSlug()).isNull();
			assertThat(item.productName()).isNull();
			assertThat(item.size()).isNull();
			assertThat(item.unitPrice()).isNull();
			assertThat(item.availableStock()).isNull();
		});
		assertThat(result.total()).isEqualByComparingTo("0");
	}

	@Test
	void keepsRequestOrderAndTotalsOnlyOkItems() {
		givenVariants(variant(1L, "S", 5), variant(2L, "M", 1), variant(3L, "L", 0), variant(4L, "XL", 10));

		CartValidationResponse result = validate(item(4L, 1), item(2L, 2), item(99L, 1), item(3L, 1), item(1L, 2));

		assertThat(result.items()).extracting(CartItemValidation::variantId, CartItemValidation::status)
			.containsExactly(tuple(4L, CartItemStatus.OK), tuple(2L, CartItemStatus.INSUFFICIENT_STOCK),
					tuple(99L, CartItemStatus.NOT_FOUND), tuple(3L, CartItemStatus.OUT_OF_STOCK),
					tuple(1L, CartItemStatus.OK));
		// (1 + 2) * 42900.00
		assertThat(result.total()).isEqualTo(new BigDecimal("128700.00"));
	}

	@Test
	void totalIsZeroWithTwoDecimalsWhenNothingIsOk() {
		givenVariants();

		assertThat(validate(item(1L, 1)).total()).isEqualTo(new BigDecimal("0.00"));
	}

	@Test
	void queriesAllRequestedIdsInOneCall() {
		givenVariants();

		validate(item(1L, 1), item(2L, 1));

		then(productVariantRepository).should().findAllWithActiveProductByIdIn(List.of(1L, 2L));
	}

	private void givenVariants(ProductVariant... variants) {
		given(productVariantRepository.findAllWithActiveProductByIdIn(anyCollection())).willReturn(List.of(variants));
	}

	private CartValidationResponse validate(CartItemRequest... items) {
		return cartValidationService.validate(new CartValidationRequest(List.of(items)));
	}

	private ProductVariant variant(Long id, String size, int stock) {
		ProductVariant variant = encaje.addVariant(size, stock);
		ReflectionTestUtils.setField(variant, "id", id);
		return variant;
	}

	private static CartItemRequest item(Long variantId, int quantity) {
		return new CartItemRequest(variantId, quantity);
	}

}
