package com.vitta.backend.admin.catalog.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vitta.backend.admin.catalog.dto.AdminImageResponse;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductImage;
import com.vitta.backend.catalog.repository.ProductImageRepository;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.common.error.ConflictException;
import com.vitta.backend.common.error.InvalidRequestException;
import com.vitta.backend.common.error.ResourceNotFoundException;
import com.vitta.backend.storage.ImageStorage;
import com.vitta.backend.storage.ImageStorage.StoredImage;
import com.vitta.backend.storage.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Product gallery: upload (JPEG/PNG/WebP checked by content, up to {@link #MAX_FILE_SIZE} each
 * and {@link #MAX_IMAGES_PER_PRODUCT} per product), reorder and delete. Positions are always
 * {@code 0..n-1}; position 0 is the cover copied to {@code products.image_url}.
 */
@Service
@Transactional
public class AdminImageService {

	public static final int MAX_IMAGES_PER_PRODUCT = 8;

	public static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(5);

	/**
	 * Temporary offset used while renumbering, so no intermediate state collides with the
	 * {@code UNIQUE (product_id, position)} constraint.
	 */
	private static final int RENUMBER_OFFSET = 1000;

	private static final Logger log = LoggerFactory.getLogger(AdminImageService.class);

	private final ProductRepository productRepository;

	private final ProductImageRepository imageRepository;

	private final ImageStorage imageStorage;

	public AdminImageService(ProductRepository productRepository, ProductImageRepository imageRepository,
			ImageStorage imageStorage) {
		this.productRepository = productRepository;
		this.imageRepository = imageRepository;
		this.imageStorage = imageStorage;
	}

	/**
	 * Validates every file first, then stores them and appends them to the gallery.
	 * @throws InvalidRequestException if there are no files, or one is empty or not a JPEG/PNG/WebP
	 * @throws MaxUploadSizeExceededException if a file exceeds {@link #MAX_FILE_SIZE}
	 * @throws ConflictException if the product would exceed {@link #MAX_IMAGES_PER_PRODUCT}
	 */
	public List<AdminImageResponse> upload(Long productId, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new InvalidRequestException("At least one image file is required");
		}
		Product product = loadProduct(productId);
		int current = product.getImages().size();
		if (current + files.size() > MAX_IMAGES_PER_PRODUCT) {
			throw new ConflictException("A product can have at most %d images (it has %d, %d sent)"
				.formatted(MAX_IMAGES_PER_PRODUCT, current, files.size()));
		}

		List<ValidatedImage> validated = files.stream().map(AdminImageService::validate).toList();

		List<StoredImage> stored = new ArrayList<>();
		try {
			for (ValidatedImage image : validated) {
				stored.add(imageStorage.store(image.content(), image.type()));
			}
			int position = product.getImages().stream().mapToInt(ProductImage::getPosition).max().orElse(-1) + 1;
			for (StoredImage image : stored) {
				product.addImage(image.url(), image.key(), position++);
			}
			product.syncCover();
			productRepository.flush();
		}
		catch (RuntimeException ex) {
			stored.forEach(image -> deleteQuietly(image.key()));
			throw ex;
		}
		return sortedImages(product);
	}

	/**
	 * @param imageIds all image ids of the product, in the new order
	 * @throws InvalidRequestException if the ids are not exactly the product's images
	 */
	public List<AdminImageResponse> reorder(Long productId, List<Long> imageIds) {
		Product product = loadProduct(productId);
		Map<Long, ProductImage> byId = product.getImages()
			.stream()
			.collect(Collectors.toMap(ProductImage::getId, Function.identity()));
		Set<Long> requested = new HashSet<>(imageIds);
		if (requested.size() != imageIds.size() || !requested.equals(byId.keySet())) {
			throw new InvalidRequestException("imageIds must list every image of the product exactly once");
		}
		renumber(imageIds.stream().map(byId::get).toList());
		product.syncCover();
		return sortedImages(product);
	}

	/**
	 * Deletes the image, closes the gap in positions and, once the transaction commits, removes
	 * the file from the storage (a storage failure is only logged).
	 */
	public void delete(Long imageId) {
		ProductImage image = imageRepository.findWithProductById(imageId)
			.orElseThrow(() -> new ResourceNotFoundException("Image", String.valueOf(imageId)));
		Product product = image.getProduct();
		product.removeImage(image);
		imageRepository.delete(image);
		imageRepository.flush();

		renumber(product.getImages().stream().sorted(Comparator.comparingInt(ProductImage::getPosition)).toList());
		product.syncCover();

		if (image.getStorageKey() != null) {
			afterCommit(() -> deleteQuietly(image.getStorageKey()));
		}
	}

	private static ValidatedImage validate(MultipartFile file) {
		if (file.isEmpty()) {
			throw new InvalidRequestException("Image file '%s' is empty".formatted(file.getOriginalFilename()));
		}
		if (file.getSize() > MAX_FILE_SIZE.toBytes()) {
			throw new MaxUploadSizeExceededException(MAX_FILE_SIZE.toBytes());
		}
		byte[] content;
		try {
			content = file.getBytes();
		}
		catch (IOException ex) {
			throw new InvalidRequestException("Could not read image file '%s'".formatted(file.getOriginalFilename()));
		}
		ImageType type = ImageType.detect(content)
			.orElseThrow(() -> new InvalidRequestException(
					"File '%s' is not a JPEG, PNG or WebP image".formatted(file.getOriginalFilename())));
		return new ValidatedImage(content, type);
	}

	/** Two-phase renumbering to {@code 0..n-1} that never violates the unique position. */
	private void renumber(List<ProductImage> ordered) {
		for (int i = 0; i < ordered.size(); i++) {
			ordered.get(i).moveTo(RENUMBER_OFFSET + i);
		}
		imageRepository.flush();
		for (int i = 0; i < ordered.size(); i++) {
			ordered.get(i).moveTo(i);
		}
		imageRepository.flush();
	}

	private Product loadProduct(Long productId) {
		return productRepository.findWithDetailsById(productId)
			.orElseThrow(() -> new ResourceNotFoundException("Product", String.valueOf(productId)));
	}

	private static List<AdminImageResponse> sortedImages(Product product) {
		return product.getImages()
			.stream()
			.sorted(Comparator.comparingInt(ProductImage::getPosition))
			.map(AdminImageResponse::from)
			.toList();
	}

	private void deleteQuietly(String key) {
		try {
			imageStorage.delete(key);
		}
		catch (RuntimeException ex) {
			log.warn("Could not delete image '{}' from storage; it is now orphaned", key, ex);
		}
	}

	private static void afterCommit(Runnable action) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			action.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				action.run();
			}
		});
	}

	private record ValidatedImage(byte[] content, ImageType type) {
	}

}
