package com.vitta.backend.admin.catalog.controller;

import java.net.URI;

import com.vitta.backend.admin.catalog.dto.AdminVariantResponse;
import com.vitta.backend.admin.catalog.dto.StockRequest;
import com.vitta.backend.admin.catalog.dto.VariantRequest;
import com.vitta.backend.admin.catalog.service.AdminVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminVariantController {

	private final AdminVariantService variantService;

	@PostMapping("/products/{productId}/variants")
	public ResponseEntity<AdminVariantResponse> create(@PathVariable Long productId,
			@Valid @RequestBody VariantRequest request) {
		AdminVariantResponse created = variantService.create(productId, request);
		return ResponseEntity.created(URI.create("/api/admin/variants/" + created.id())).body(created);
	}

	@PutMapping("/variants/{id}")
	public AdminVariantResponse update(@PathVariable Long id, @Valid @RequestBody VariantRequest request) {
		return variantService.update(id, request);
	}

	@PatchMapping("/variants/{id}/stock")
	public AdminVariantResponse changeStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
		return variantService.changeStock(id, request.stock());
	}

	@DeleteMapping("/variants/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		variantService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
