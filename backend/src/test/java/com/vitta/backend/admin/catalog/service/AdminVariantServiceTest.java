package com.vitta.backend.admin.catalog.service;

import java.math.BigDecimal;
import java.util.Optional;

import com.vitta.backend.admin.catalog.dto.AdminVariantResponse;
import com.vitta.backend.admin.catalog.dto.VariantRequest;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductVariant;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.catalog.repository.ProductVariantRepository;
import com.vitta.backend.common.error.ConflictException;
import com.vitta.backend.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminVariantServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductVariantRepository variantRepository;

	@InjectMocks
	private AdminVariantService service;

	private Product product;

	@BeforeEach
	void setUp() {
		product = new Product("Body", "body", null, BigDecimal.TEN, new Category("Bodies", "bodies"), null, true);
		ReflectionTestUtils.setField(product, "id", 7L);
	}

	@Test
	void createNormalizesSize() {
		given(productRepository.findById(7L)).willReturn(Optional.of(product));
		given(variantRepository.existsByProductIdAndSizeIgnoreCase(7L, "XL")).willReturn(false);
		given(variantRepository.save(any(ProductVariant.class))).willAnswer(invocation -> invocation.getArgument(0));

		AdminVariantResponse created = service.create(7L, new VariantRequest(" xl ", 3));

		assertThat(created.size()).isEqualTo("XL");
		assertThat(created.stock()).isEqualTo(3);
		assertThat(product.getVariants()).hasSize(1);
	}

	@Test
	void createRejectsRepeatedSize() {
		given(productRepository.findById(7L)).willReturn(Optional.of(product));
		given(variantRepository.existsByProductIdAndSizeIgnoreCase(7L, "M")).willReturn(true);

		assertThatThrownBy(() -> service.create(7L, new VariantRequest("m", 1))).isInstanceOf(ConflictException.class)
			.hasMessageContaining("'M'");
		then(variantRepository).should(never()).save(any());
	}

	@Test
	void createFailsForUnknownProduct() {
		given(productRepository.findById(7L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(7L, new VariantRequest("M", 1)))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateRejectsSizeOfAnotherVariant() {
		ProductVariant variant = product.addVariant("S", 1);
		given(variantRepository.findWithProductById(3L)).willReturn(Optional.of(variant));
		given(variantRepository.existsByProductIdAndSizeIgnoreCaseAndIdNot(7L, "M", 3L)).willReturn(true);

		assertThatThrownBy(() -> service.update(3L, new VariantRequest("M", 1)))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void changeStockUpdatesOnlyStock() {
		ProductVariant variant = product.addVariant("S", 1);
		given(variantRepository.findWithProductById(3L)).willReturn(Optional.of(variant));

		AdminVariantResponse updated = service.changeStock(3L, 9999);

		assertThat(updated.stock()).isEqualTo(9999);
		assertThat(updated.size()).isEqualTo("S");
	}

	@Test
	void deleteRemovesVariant() {
		ProductVariant variant = product.addVariant("S", 1);
		given(variantRepository.findWithProductById(3L)).willReturn(Optional.of(variant));

		service.delete(3L);

		then(variantRepository).should().delete(variant);
	}

	@Test
	void deleteFailsForUnknownVariant() {
		given(variantRepository.findWithProductById(3L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(3L)).isInstanceOf(ResourceNotFoundException.class);
	}

}
