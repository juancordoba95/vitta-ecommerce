package com.vitta.backend.admin.catalog.service;

import java.util.Locale;

import com.vitta.backend.admin.catalog.dto.AdminProductDetailResponse;
import com.vitta.backend.admin.catalog.dto.AdminProductSummaryResponse;
import com.vitta.backend.admin.catalog.dto.PageResponse;
import com.vitta.backend.admin.catalog.dto.ProductRequest;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.repository.CategoryRepository;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.catalog.service.SlugGenerator;
import com.vitta.backend.common.error.ConflictException;
import com.vitta.backend.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProductService {

	static final int SLUG_MAX_LENGTH = 170;

	private final ProductRepository productRepository;

	private final CategoryRepository categoryRepository;

	/**
	 * Pages through all products (active and inactive), sorted by name. {@code text} matches
	 * the name (case-insensitive, contains); {@code null} filters are ignored.
	 */
	@Transactional(readOnly = true)
	public PageResponse<AdminProductSummaryResponse> search(String text, Long categoryId, int page, int size) {
		String namePattern = StringUtils.hasText(text) ? "%" + escapeLike(text.strip().toLowerCase(Locale.ROOT)) + "%"
				: null;
		PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending().and(Sort.by("id")));
		return PageResponse.from(productRepository.searchAdmin(namePattern, categoryId, pageable),
				AdminProductSummaryResponse::from);
	}

	@Transactional(readOnly = true)
	public AdminProductDetailResponse findById(Long id) {
		return AdminProductDetailResponse.from(loadWithDetails(id));
	}

	/**
	 * Creates an <b>inactive</b> product, so it is not public until it is activated.
	 * @throws ResourceNotFoundException if the category does not exist
	 * @throws ConflictException if an explicit slug is already used
	 */
	public AdminProductDetailResponse create(ProductRequest request) {
		Category category = loadCategory(request.categoryId());
		String name = request.name().strip();
		String slug;
		if (StringUtils.hasText(request.slug())) {
			slug = request.slug();
			if (productRepository.existsBySlug(slug)) {
				throw slugTaken(slug);
			}
		}
		else {
			slug = SlugGenerator.unique(SlugGenerator.slugify(name, SLUG_MAX_LENGTH, "producto"),
					productRepository::existsBySlug);
		}
		Product product = new Product(name, slug, description(request), request.price(), category, null, false);
		return AdminProductDetailResponse.from(productRepository.save(product));
	}

	/**
	 * Updates the product data; an empty slug keeps the current one (so shared links keep
	 * working).
	 * @throws ResourceNotFoundException if the product or the category does not exist
	 * @throws ConflictException if the new slug is used by another product
	 */
	public AdminProductDetailResponse update(Long id, ProductRequest request) {
		Product product = loadWithDetails(id);
		Category category = loadCategory(request.categoryId());
		String slug = product.getSlug();
		if (StringUtils.hasText(request.slug()) && !request.slug().equals(slug)) {
			if (productRepository.existsBySlugAndIdNot(request.slug(), id)) {
				throw slugTaken(request.slug());
			}
			slug = request.slug();
		}
		product.update(request.name().strip(), slug, description(request), request.price(), category);
		return AdminProductDetailResponse.from(product);
	}

	/** Logical delete/restore: inactive products are hidden from the public catalog and cart. */
	public AdminProductDetailResponse setActive(Long id, boolean active) {
		Product product = loadWithDetails(id);
		product.setActive(active);
		return AdminProductDetailResponse.from(product);
	}

	private Product loadWithDetails(Long id) {
		return productRepository.findWithDetailsById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Product", String.valueOf(id)));
	}

	private Category loadCategory(Long id) {
		return categoryRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Category", String.valueOf(id)));
	}

	private static String description(ProductRequest request) {
		return StringUtils.hasText(request.description()) ? request.description().strip() : null;
	}

	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static ConflictException slugTaken(String slug) {
		return new ConflictException("Product slug '%s' is already in use".formatted(slug));
	}

}
