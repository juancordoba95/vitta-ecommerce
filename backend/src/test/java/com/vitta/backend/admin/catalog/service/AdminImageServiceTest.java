package com.vitta.backend.admin.catalog.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.vitta.backend.admin.catalog.dto.AdminImageResponse;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductImage;
import com.vitta.backend.catalog.repository.ProductImageRepository;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.common.error.ConflictException;
import com.vitta.backend.common.error.InvalidRequestException;
import com.vitta.backend.common.error.ResourceNotFoundException;
import com.vitta.backend.storage.ImageStorage;
import com.vitta.backend.storage.ImageStorage.StoredImage;
import com.vitta.backend.storage.ImageStorageException;
import com.vitta.backend.storage.ImageType;
import com.vitta.backend.storage.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AdminImageServiceTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private ProductImageRepository imageRepository;

	@Mock
	private ImageStorage imageStorage;

	private AdminImageService service;

	private Product product;

	@BeforeEach
	void setUp() {
		service = new AdminImageService(productRepository, imageRepository, imageStorage);
		product = new Product("Body", "body", null, BigDecimal.TEN, new Category("Bodies", "bodies"), null, true);
		ReflectionTestUtils.setField(product, "id", 7L);
	}

	@Test
	void uploadAppendsImagesAndSetsCover() {
		given(productRepository.findWithDetailsById(7L)).willReturn(Optional.of(product));
		given(imageStorage.store(TestImages.PNG, ImageType.PNG)).willReturn(new StoredImage("https://cdn/a.png", "a"));
		given(imageStorage.store(TestImages.WEBP, ImageType.WEBP))
			.willReturn(new StoredImage("https://cdn/b.webp", "b"));

		List<AdminImageResponse> images = service.upload(7L, List.of(file("a.png", TestImages.PNG),
				// The extension is ignored: the content decides.
				file("b.jpg", TestImages.WEBP)));

		assertThat(images).extracting(AdminImageResponse::url, AdminImageResponse::position)
			.containsExactly(tuple("https://cdn/a.png", 0), tuple("https://cdn/b.webp", 1));
		assertThat(product.getImageUrl()).isEqualTo("https://cdn/a.png");
	}

	@Test
	void uploadRejectsContentThatIsNotAnImageBeforeStoringAnything() {
		given(productRepository.findWithDetailsById(7L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> service.upload(7L,
				List.of(file("ok.png", TestImages.PNG), file("fake.jpg", TestImages.TEXT))))
			.isInstanceOf(InvalidRequestException.class)
			.hasMessageContaining("fake.jpg");
		then(imageStorage).shouldHaveNoInteractions();
	}

	@Test
	void uploadRejectsFilesOverFiveMegabytes() {
		given(productRepository.findWithDetailsById(7L)).willReturn(Optional.of(product));
		byte[] big = new byte[(int) AdminImageService.MAX_FILE_SIZE.toBytes() + 1];
		System.arraycopy(TestImages.PNG, 0, big, 0, TestImages.PNG.length);

		assertThatThrownBy(() -> service.upload(7L, List.of(file("big.png", big))))
			.isInstanceOf(MaxUploadSizeExceededException.class);
	}

	@Test
	void uploadRejectsMoreThanEightImagesPerProduct() {
		for (int i = 0; i < 7; i++) {
			product.addImage("https://cdn/" + i, i);
		}
		given(productRepository.findWithDetailsById(7L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> service.upload(7L,
				List.of(file("a.png", TestImages.PNG), file("b.png", TestImages.PNG))))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void uploadCleansUpStoredFilesWhenAStoreFails() {
		given(productRepository.findWithDetailsById(7L)).willReturn(Optional.of(product));
		given(imageStorage.store(TestImages.PNG, ImageType.PNG)).willReturn(new StoredImage("https://cdn/a.png", "a"));
		given(imageStorage.store(TestImages.JPEG, ImageType.JPEG))
			.willThrow(new ImageStorageException("down", null));

		assertThatThrownBy(() -> service.upload(7L,
				List.of(file("a.png", TestImages.PNG), file("b.jpg", TestImages.JPEG))))
			.isInstanceOf(ImageStorageException.class);
		then(imageStorage).should().delete("a");
	}

	@Test
	void uploadFailsForUnknownProduct() {
		given(productRepository.findWithDetailsById(7L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> service.upload(7L, List.of(file("a.png", TestImages.PNG))))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void reorderRenumbersAndMovesCover() {
		ProductImage first = image(1L, "https://cdn/1", 0);
		ProductImage second = image(2L, "https://cdn/2", 1);
		ProductImage third = image(3L, "https://cdn/3", 2);
		given(productRepository.findWithDetailsById(7L)).willReturn(Optional.of(product));

		List<AdminImageResponse> images = service.reorder(7L, List.of(3L, 1L, 2L));

		assertThat(images).extracting(AdminImageResponse::id).containsExactly(3L, 1L, 2L);
		assertThat(List.of(third.getPosition(), first.getPosition(), second.getPosition())).containsExactly(0, 1, 2);
		assertThat(product.getImageUrl()).isEqualTo("https://cdn/3");
	}

	@Test
	void reorderRejectsIdsThatDoNotMatchTheGallery() {
		image(1L, "https://cdn/1", 0);
		image(2L, "https://cdn/2", 1);
		given(productRepository.findWithDetailsById(7L)).willReturn(Optional.of(product));

		assertThatThrownBy(() -> service.reorder(7L, List.of(1L))).isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> service.reorder(7L, List.of(1L, 1L))).isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> service.reorder(7L, List.of(1L, 99L))).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void deleteCompactsPositionsUpdatesCoverAndDeletesFromStorage() {
		ProductImage cover = image(1L, "https://cdn/1", 0);
		ReflectionTestUtils.setField(cover, "storageKey", "key-1");
		ProductImage second = image(2L, "https://cdn/2", 1);
		given(imageRepository.findWithProductById(1L)).willReturn(Optional.of(cover));

		service.delete(1L);

		then(imageRepository).should().delete(cover);
		assertThat(product.getImages()).containsExactly(second);
		assertThat(second.getPosition()).isZero();
		assertThat(product.getImageUrl()).isEqualTo("https://cdn/2");
		then(imageStorage).should().delete("key-1");
	}

	@Test
	void deleteOfExternalImageDoesNotTouchStorage() {
		image(1L, "https://placehold.co/x", 0);
		given(imageRepository.findWithProductById(1L)).willReturn(Optional.of(product.getImages().iterator().next()));

		service.delete(1L);

		then(imageStorage).should(never()).delete(any());
		assertThat(product.getImageUrl()).isNull();
	}

	private ProductImage image(Long id, String url, int position) {
		ProductImage image = product.addImage(url, position);
		ReflectionTestUtils.setField(image, "id", id);
		return image;
	}

	private static MultipartFile file(String name, byte[] content) {
		return new MockMultipartFile("files", name, "application/octet-stream", content);
	}

}
