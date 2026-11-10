package com.vitta.backend.catalog.service;

import java.util.List;

import com.vitta.backend.catalog.dto.CategoryResponse;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private CategoryService categoryService;

	@Test
	void findAllMapsCategoriesInRepositoryOrder() {
		given(categoryRepository.findAllByOrderByNameAsc())
			.willReturn(List.of(new Category("Bodies", "bodies"), new Category("Conjuntos", "conjuntos")));

		List<CategoryResponse> result = categoryService.findAll();

		assertThat(result).extracting(CategoryResponse::name, CategoryResponse::slug)
			.containsExactly(tuple("Bodies", "bodies"), tuple("Conjuntos", "conjuntos"));
	}

	@Test
	void findAllReturnsEmptyListWhenThereAreNoCategories() {
		given(categoryRepository.findAllByOrderByNameAsc()).willReturn(List.of());

		assertThat(categoryService.findAll()).isEmpty();
	}

}
