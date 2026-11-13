package com.vitta.backend.admin.catalog.service;

import java.util.List;

import com.vitta.backend.admin.catalog.dto.CategoryRequest;
import com.vitta.backend.catalog.dto.CategoryResponse;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.repository.CategoryRepository;
import com.vitta.backend.catalog.service.SlugGenerator;
import com.vitta.backend.common.error.ConflictException;
import com.vitta.backend.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminCategoryService {

	static final int SLUG_MAX_LENGTH = 120;

	private final CategoryRepository categoryRepository;

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return categoryRepository.findAllByOrderByNameAsc().stream().map(CategoryResponse::from).toList();
	}

	/**
	 * @throws ConflictException if an explicit slug is already used
	 */
	public CategoryResponse create(CategoryRequest request) {
		String name = request.name().strip();
		String slug;
		if (StringUtils.hasText(request.slug())) {
			slug = request.slug();
			if (categoryRepository.existsBySlug(slug)) {
				throw slugTaken(slug);
			}
		}
		else {
			slug = SlugGenerator.unique(SlugGenerator.slugify(name, SLUG_MAX_LENGTH, "categoria"),
					categoryRepository::existsBySlug);
		}
		return CategoryResponse.from(categoryRepository.save(new Category(name, slug)));
	}

	/**
	 * Updates name and, if given, slug; an empty slug keeps the current one.
	 * @throws ResourceNotFoundException if the category does not exist
	 * @throws ConflictException if the new slug is used by another category
	 */
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = categoryRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Category", String.valueOf(id)));
		String slug = category.getSlug();
		if (StringUtils.hasText(request.slug()) && !request.slug().equals(slug)) {
			if (categoryRepository.existsBySlugAndIdNot(request.slug(), id)) {
				throw slugTaken(request.slug());
			}
			slug = request.slug();
		}
		category.update(request.name().strip(), slug);
		return CategoryResponse.from(category);
	}

	private static ConflictException slugTaken(String slug) {
		return new ConflictException("Category slug '%s' is already in use".formatted(slug));
	}

}
