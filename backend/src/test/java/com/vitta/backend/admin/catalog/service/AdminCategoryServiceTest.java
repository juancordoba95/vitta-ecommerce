package com.vitta.backend.admin.catalog.service;

import java.util.Optional;

import com.vitta.backend.admin.catalog.dto.CategoryRequest;
import com.vitta.backend.catalog.dto.CategoryResponse;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.repository.CategoryRepository;
import com.vitta.backend.common.error.ConflictException;
import com.vitta.backend.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private AdminCategoryService service;

	@Test
	void createGeneratesUniqueSlugFromName() {
		given(categoryRepository.existsBySlug(anyString())).willReturn(false);
		given(categoryRepository.existsBySlug("ropa-interior")).willReturn(true);
		given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> invocation.getArgument(0));

		CategoryResponse created = service.create(new CategoryRequest("Ropa Interior", null));

		assertThat(created.slug()).isEqualTo("ropa-interior-2");
	}

	@Test
	void createRejectsExplicitSlugInUse() {
		given(categoryRepository.existsBySlug("bodies")).willReturn(true);

		assertThatThrownBy(() -> service.create(new CategoryRequest("Bodies", "bodies")))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void updateKeepsSlugWhenEmptyAndRenames() {
		given(categoryRepository.findById(1L)).willReturn(Optional.of(new Category("Bodys", "bodies")));

		CategoryResponse updated = service.update(1L, new CategoryRequest("Bodies", ""));

		assertThat(updated.name()).isEqualTo("Bodies");
		assertThat(updated.slug()).isEqualTo("bodies");
	}

	@Test
	void updateRejectsSlugOfAnotherCategory() {
		given(categoryRepository.findById(1L)).willReturn(Optional.of(new Category("Bodies", "bodies")));
		given(categoryRepository.existsBySlugAndIdNot("conjuntos", 1L)).willReturn(true);

		assertThatThrownBy(() -> service.update(1L, new CategoryRequest("Bodies", "conjuntos")))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void updateFailsForUnknownCategory() {
		given(categoryRepository.findById(1L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(1L, new CategoryRequest("X", null)))
			.isInstanceOf(ResourceNotFoundException.class);
	}

}
