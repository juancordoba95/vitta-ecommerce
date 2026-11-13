package com.vitta.backend.admin.catalog.service;

import java.util.Locale;

import com.vitta.backend.admin.catalog.dto.AdminVariantResponse;
import com.vitta.backend.admin.catalog.dto.VariantRequest;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductVariant;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.catalog.repository.ProductVariantRepository;
import com.vitta.backend.common.error.ConflictException;
import com.vitta.backend.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sizes and stock. Sizes are stored trimmed and uppercase, and are unique per product
 * (case-insensitive).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminVariantService {

	private final ProductRepository productRepository;

	private final ProductVariantRepository variantRepository;

	public AdminVariantResponse create(Long productId, VariantRequest request) {
		Product product = productRepository.findById(productId)
			.orElseThrow(() -> new ResourceNotFoundException("Product", String.valueOf(productId)));
		String size = normalizeSize(request.size());
		if (variantRepository.existsByProductIdAndSizeIgnoreCase(productId, size)) {
			throw sizeTaken(size);
		}
		ProductVariant variant = product.addVariant(size, request.stock());
		return AdminVariantResponse.from(variantRepository.save(variant));
	}

	public AdminVariantResponse update(Long id, VariantRequest request) {
		ProductVariant variant = load(id);
		String size = normalizeSize(request.size());
		if (variantRepository.existsByProductIdAndSizeIgnoreCaseAndIdNot(variant.getProduct().getId(), size, id)) {
			throw sizeTaken(size);
		}
		variant.update(size, request.stock());
		return AdminVariantResponse.from(variant);
	}

	public AdminVariantResponse changeStock(Long id, int stock) {
		ProductVariant variant = load(id);
		variant.changeStock(stock);
		return AdminVariantResponse.from(variant);
	}

	/** Physical delete: carts holding this variant will get {@code NOT_FOUND} on validation. */
	public void delete(Long id) {
		variantRepository.delete(load(id));
	}

	static String normalizeSize(String size) {
		return size.strip().toUpperCase(Locale.ROOT);
	}

	private ProductVariant load(Long id) {
		return variantRepository.findWithProductById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Variant", String.valueOf(id)));
	}

	private static ConflictException sizeTaken(String size) {
		return new ConflictException("Size '%s' already exists for this product".formatted(size));
	}

}
