package com.vitta.backend.admin.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.vitta.backend.admin.catalog.dto.AdminProductDetailResponse;
import com.vitta.backend.admin.catalog.dto.ProductRequest;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.repository.CategoryRepository;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.common.error.ConflictException;
import com.vitta.backend.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceTest {

	private static final BigDecimal PRICE = new BigDecimal("42900.00");

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private AdminProductService service;

	private Category category;

	@BeforeEach
	void setUp() {
		category = new Category("Conjuntos", "conjuntos");
		ReflectionTestUtils.setField(category, "id", 1L);
	}

	@Test
	void createGeneratesSlugWithSuffixAndStartsInactive() {
		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
		given(productRepository.existsBySlug(anyString())).willReturn(false);
		given(productRepository.existsBySlug("conjunto-encaje")).willReturn(true);
		given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));

		AdminProductDetailResponse created = service
			.create(new ProductRequest(" Conjunto Encaje ", "  ", PRICE, 1L, ""));

		assertThat(created.slug()).isEqualTo("conjunto-encaje-2");
		assertThat(created.name()).isEqualTo("Conjunto Encaje");
		assertThat(created.description()).isNull();
		assertThat(created.active()).isFalse();
		assertThat(created.category().slug()).isEqualTo("conjuntos");
	}

	@Test
	void createRejectsExplicitSlugInUse() {
		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
		given(productRepository.existsBySlug("conjunto")).willReturn(true);

		assertThatThrownBy(() -> service.create(new ProductRequest("Conjunto", null, PRICE, 1L, "conjunto")))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("conjunto");
		then(productRepository).should(never()).save(any());
	}

	@Test
	void createFailsForUnknownCategory() {
		given(categoryRepository.findById(9L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(new ProductRequest("Conjunto", null, PRICE, 9L, null)))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateWithEmptySlugKeepsCurrentOne() {
		Product product = product("conjunto-viejo");
		given(productRepository.findWithDetailsById(5L)).willReturn(Optional.of(product));
		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

		AdminProductDetailResponse updated = service
			.update(5L, new ProductRequest("Nombre Nuevo", "Desc", PRICE, 1L, null));

		assertThat(updated.slug()).isEqualTo("conjunto-viejo");
		assertThat(updated.name()).isEqualTo("Nombre Nuevo");
		then(productRepository).should(never()).existsBySlugAndIdNot(anyString(), any());
	}

	@Test
	void updateRejectsSlugOfAnotherProduct() {
		given(productRepository.findWithDetailsById(5L)).willReturn(Optional.of(product("conjunto-viejo")));
		given(categoryRepository.findById(1L)).willReturn(Optional.of(category));
		given(productRepository.existsBySlugAndIdNot("otro", 5L)).willReturn(true);

		assertThatThrownBy(() -> service.update(5L, new ProductRequest("X", null, PRICE, 1L, "otro")))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void updateFailsForUnknownProduct() {
		given(productRepository.findWithDetailsById(5L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(5L, new ProductRequest("X", null, PRICE, 1L, null)))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void setActiveTogglesVisibility() {
		given(productRepository.findWithDetailsById(5L)).willReturn(Optional.of(product("p")));

		assertThat(service.setActive(5L, true).active()).isTrue();
	}

	@Test
	void searchEscapesLikeWildcardsAndLowercases() {
		Page<Product> empty = new PageImpl<>(List.of());
		given(productRepository.searchAdmin(eq("%100!%!_algod!_n!!%"), eq(3L), any(Pageable.class)))
			.willReturn(empty);

		assertThat(service.search(" 100%_ALGOD_N! ", 3L, 0, 20).totalElements()).isZero();
	}

	private Product product(String slug) {
		Product product = new Product("Viejo", slug, null, PRICE, category, null, false);
		ReflectionTestUtils.setField(product, "id", 5L);
		return product;
	}

}
