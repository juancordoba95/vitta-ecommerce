package com.vitta.backend.catalog.controller;

import java.util.List;

import com.vitta.backend.catalog.dto.ProductDetailResponse;
import com.vitta.backend.catalog.dto.ProductSummaryResponse;
import com.vitta.backend.catalog.service.ProductService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

	private static final String SLUG_PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

	private static final String SLUG_MESSAGE = "must be a lowercase slug (letters, digits and hyphens)";

	private final ProductService productService;

	@GetMapping
	public List<ProductSummaryResponse> findAll(@RequestParam(name = "category", required = false) @Size(max = 120)
	@Pattern(regexp = SLUG_PATTERN, message = SLUG_MESSAGE) String category) {
		return productService.findActive(category);
	}

	@GetMapping("/{slug}")
	public ProductDetailResponse findBySlug(
			@PathVariable @Size(max = 170) @Pattern(regexp = SLUG_PATTERN, message = SLUG_MESSAGE) String slug) {
		return productService.findActiveBySlug(slug);
	}

}
