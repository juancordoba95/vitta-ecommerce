package com.vitta.backend.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.vitta.backend.catalog.dto.ProductDetailResponse;
import com.vitta.backend.catalog.dto.ProductSummaryResponse;
import com.vitta.backend.catalog.dto.ProductVariantResponse;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.repository.CategoryRepository;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	private final Category conjuntos = new Category("Conjuntos", "conjuntos");

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private ProductService productService;

	@Test
	void findActiveWithoutCategoryListsAllActiveProducts() {
		given(productRepository.findAllByActiveTrueOrderByNameAsc()).willReturn(List.of(product("conjunto-encaje")));

		List<ProductSummaryResponse> result = productService.findActive(null);

		assertThat(result).singleElement().satisfies((summary) -> {
			assertThat(summary.slug()).isEqualTo("conjunto-encaje");
			assertThat(summary.name()).isEqualTo("Product conjunto-encaje");
			assertThat(summary.price()).isEqualByComparingTo("42900.00");
			assertThat(summary.imageUrl()).isEqualTo("https://placehold.co/600x800");
			assertThat(summary.categorySlug()).isEqualTo("conjuntos");
		});
		then(categoryRepository).shouldHaveNoInteractions();
	}

	@Test
	void findActiveWithExistingCategoryFiltersBySlug() {
		given(categoryRepository.existsBySlug("conjuntos")).willReturn(true);
		given(productRepository.findAllByActiveTrueAndCategorySlugOrderByNameAsc("conjuntos"))
			.willReturn(List.of(product("conjunto-encaje"), product("conjunto-tul")));

		List<ProductSummaryResponse> result = productService.findActive("conjuntos");

		assertThat(result).extracting(ProductSummaryResponse::slug).containsExactly("conjunto-encaje", "conjunto-tul");
		then(productRepository).should(never()).findAllByActiveTrueOrderByNameAsc();
	}

	@Test
	void findActiveWithUnknownCategoryThrowsNotFound() {
		given(categoryRepository.existsBySlug("unknown")).willReturn(false);

		assertThatThrownBy(() -> productService.findActive("unknown")).isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Category 'unknown' not found");
		then(productRepository).shouldHaveNoInteractions();
	}

	@Test
	void findActiveBySlugReturnsDetailWithCategoryAndVariants() {
		Product product = product("conjunto-encaje");
		product.addVariant("S", 4);
		product.addVariant("M", 0);
		given(productRepository.findBySlugAndActiveTrue("conjunto-encaje")).willReturn(Optional.of(product));

		ProductDetailResponse result = productService.findActiveBySlug("conjunto-encaje");

		assertThat(result.slug()).isEqualTo("conjunto-encaje");
		assertThat(result.description()).isEqualTo("Description");
		assertThat(result.category().slug()).isEqualTo("conjuntos");
		assertThat(result.category().name()).isEqualTo("Conjuntos");
		assertThat(result.variants()).containsExactly(new ProductVariantResponse("S", 4),
				new ProductVariantResponse("M", 0));
	}

	@Test
	void findActiveBySlugThrowsNotFoundWhenMissingOrInactive() {
		given(productRepository.findBySlugAndActiveTrue(anyString())).willReturn(Optional.empty());

		assertThatThrownBy(() -> productService.findActiveBySlug("missing"))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Product 'missing' not found");
	}

	private Product product(String slug) {
		return new Product("Product " + slug, slug, "Description", new BigDecimal("42900.00"), conjuntos,
				"https://placehold.co/600x800", true);
	}

}
