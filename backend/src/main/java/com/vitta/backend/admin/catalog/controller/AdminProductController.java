package com.vitta.backend.admin.catalog.controller;

import java.net.URI;

import com.vitta.backend.admin.catalog.dto.AdminProductDetailResponse;
import com.vitta.backend.admin.catalog.dto.AdminProductSummaryResponse;
import com.vitta.backend.admin.catalog.dto.PageResponse;
import com.vitta.backend.admin.catalog.dto.ProductActiveRequest;
import com.vitta.backend.admin.catalog.dto.ProductRequest;
import com.vitta.backend.admin.catalog.service.AdminProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

	private final AdminProductService productService;

	@GetMapping
	public PageResponse<AdminProductSummaryResponse> search(
			@RequestParam(name = "q", required = false) @Size(max = 150) String text,
			@RequestParam(required = false) @Positive Long categoryId,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return productService.search(text, categoryId, page, size);
	}

	@GetMapping("/{id}")
	public AdminProductDetailResponse findById(@PathVariable Long id) {
		return productService.findById(id);
	}

	@PostMapping
	public ResponseEntity<AdminProductDetailResponse> create(@Valid @RequestBody ProductRequest request) {
		AdminProductDetailResponse created = productService.create(request);
		return ResponseEntity.created(URI.create("/api/admin/products/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public AdminProductDetailResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return productService.update(id, request);
	}

	@PatchMapping("/{id}/active")
	public AdminProductDetailResponse setActive(@PathVariable Long id,
			@Valid @RequestBody ProductActiveRequest request) {
		return productService.setActive(id, request.active());
	}

}
