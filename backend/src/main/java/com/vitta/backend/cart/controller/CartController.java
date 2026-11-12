package com.vitta.backend.cart.controller;

import com.vitta.backend.cart.dto.CartValidationRequest;
import com.vitta.backend.cart.dto.CartValidationResponse;
import com.vitta.backend.cart.service.CartValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartValidationService cartValidationService;

	@PostMapping("/validate")
	public CartValidationResponse validate(@Valid @RequestBody CartValidationRequest request) {
		return cartValidationService.validate(request);
	}

}
