package com.vitta.backend.cart.controller;

import java.math.BigDecimal;
import java.util.List;

import com.vitta.backend.cart.dto.CartItemRequest;
import com.vitta.backend.cart.dto.CartItemStatus;
import com.vitta.backend.cart.dto.CartItemValidation;
import com.vitta.backend.cart.dto.CartValidationRequest;
import com.vitta.backend.cart.dto.CartValidationResponse;
import com.vitta.backend.cart.service.CartValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CartValidationService cartValidationService;

	@Test
	void returnsOkEvenWhenSomeItemsHaveProblems() throws Exception {
		CartValidationRequest request = new CartValidationRequest(
				List.of(new CartItemRequest(10L, 2), new CartItemRequest(99L, 1)));
		given(cartValidationService.validate(request)).willReturn(new CartValidationResponse(List.of(
				new CartItemValidation(10L, "conjunto-encaje", "Conjunto Encaje", "S", new BigDecimal("42900.00"), 2,
						4, CartItemStatus.OK),
				new CartItemValidation(99L, null, null, null, null, 1, null, CartItemStatus.NOT_FOUND)),
				new BigDecimal("85800.00")));

		mockMvc.perform(post("/api/cart/validate").contentType(MediaType.APPLICATION_JSON).content("""
				{ "items": [ { "variantId": 10, "quantity": 2 }, { "variantId": 99, "quantity": 1 } ] }
				"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.items[0].variantId").value(10))
			.andExpect(jsonPath("$.items[0].productSlug").value("conjunto-encaje"))
			.andExpect(jsonPath("$.items[0].productName").value("Conjunto Encaje"))
			.andExpect(jsonPath("$.items[0].size").value("S"))
			.andExpect(jsonPath("$.items[0].unitPrice").value(42900.0))
			.andExpect(jsonPath("$.items[0].quantity").value(2))
			.andExpect(jsonPath("$.items[0].availableStock").value(4))
			.andExpect(jsonPath("$.items[0].status").value("OK"))
			.andExpect(jsonPath("$.items[1].variantId").value(99))
			.andExpect(jsonPath("$.items[1].status").value("NOT_FOUND"))
			.andExpect(jsonPath("$.total").value(85800.0));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", //
			"{ \"items\": [] }", //
			"{ \"items\": [null] }", //
			"{ \"items\": [ { \"quantity\": 1 } ] }", //
			"{ \"items\": [ { \"variantId\": 1 } ] }", //
			"{ \"items\": [ { \"variantId\": 1, \"quantity\": 0 } ] }", //
			"{ \"items\": [ { \"variantId\": 1, \"quantity\": -2 } ] }", //
			"{ \"items\": [ { \"variantId\": 1, \"quantity\": 1 }, { \"variantId\": 1, \"quantity\": 2 } ] }", //
			"{ \"items\": [ { \"variantId\": \"abc\", \"quantity\": 1 } ] }", //
			"not json" })
	void returnsBadRequestForInvalidBody(String body) throws Exception {
		mockMvc.perform(post("/api/cart/validate").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
		then(cartValidationService).shouldHaveNoInteractions();
	}

	@Test
	void returnsBadRequestWithoutBody() throws Exception {
		mockMvc.perform(post("/api/cart/validate").contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isBadRequest());
		then(cartValidationService).shouldHaveNoInteractions();
	}

}
