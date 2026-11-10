package com.vitta.backend.catalog.service;

import java.util.List;

import com.vitta.backend.catalog.dto.CategoryResponse;
import com.vitta.backend.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categoryRepository;

	public List<CategoryResponse> findAll() {
		return categoryRepository.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
	}

}
