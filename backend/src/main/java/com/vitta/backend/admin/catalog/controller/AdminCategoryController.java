package com.vitta.backend.admin.catalog.controller;

import java.net.URI;
import java.util.List;

import com.vitta.backend.admin.catalog.dto.CategoryRequest;
import com.vitta.backend.admin.catalog.service.AdminCategoryService;
import com.vitta.backend.catalog.dto.CategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Categories have no delete: products always need one. */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

	private final AdminCategoryService categoryService;

	@GetMapping
	public List<CategoryResponse> findAll() {
		return categoryService.findAll();
	}

	@PostMapping
	public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
		CategoryResponse created = categoryService.create(request);
		return ResponseEntity.created(URI.create("/api/admin/categories/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return categoryService.update(id, request);
	}

}
