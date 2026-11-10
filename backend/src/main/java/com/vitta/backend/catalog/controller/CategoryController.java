package com.vitta.backend.catalog.controller;

import java.util.List;

import com.vitta.backend.catalog.dto.CategoryResponse;
import com.vitta.backend.catalog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	@GetMapping
	public List<CategoryResponse> findAll() {
		return categoryService.findAll();
	}

}
