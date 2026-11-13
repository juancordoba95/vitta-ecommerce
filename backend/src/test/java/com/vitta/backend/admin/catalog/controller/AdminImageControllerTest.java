package com.vitta.backend.admin.catalog.controller;

import java.math.BigDecimal;
import java.util.Optional;

import com.vitta.backend.admin.catalog.service.AdminImageService;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductImage;
import com.vitta.backend.catalog.repository.ProductImageRepository;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.common.security.SecurityConfig;
import com.vitta.backend.storage.ImageStorage;
import com.vitta.backend.storage.ImageStorage.StoredImage;
import com.vitta.backend.storage.ImageType;
import com.vitta.backend.storage.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static com.vitta.backend.admin.AdminTestAuth.admin;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real {@link AdminImageService} (content type, size and count rules) with mocked
 * repositories and storage.
 */
@WebMvcTest(AdminImageController.class)
@Import({ SecurityConfig.class, AdminImageService.class })
class AdminImageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductRepository productRepository;

	@MockitoBean
	private ProductImageRepository imageRepository;

	@MockitoBean
	private ImageStorage imageStorage;

	private Product product;

	@BeforeEach
	void setUp() {
		product = new Product("Body", "body", null, BigDecimal.TEN, new Category("Bodies", "bodies"), null, true);
		ReflectionTestUtils.setField(product, "id", 7L);
		given(productRepository.findWithDetailsById(7L)).willReturn(Optional.of(product));
	}

	@Test
	void uploadStoresValidImages() throws Exception {
		given(imageStorage.store(TestImages.PNG, ImageType.PNG)).willReturn(new StoredImage("https://cdn/a.png", "a"));
		given(imageStorage.store(TestImages.JPEG, ImageType.JPEG))
			.willReturn(new StoredImage("https://cdn/b.jpg", "b"));

		mockMvc.perform(multipart("/api/admin/products/7/images").file(file("a.png", TestImages.PNG))
			.file(file("b.jpeg", TestImages.JPEG))
			.with(admin()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].url").value("https://cdn/a.png"))
			.andExpect(jsonPath("$[0].position").value(0))
			.andExpect(jsonPath("$[1].position").value(1));
	}

	@Test
	void uploadRejectsFileWhoseContentIsNotAnImage() throws Exception {
		mockMvc.perform(multipart("/api/admin/products/7/images")
			.file(new MockMultipartFile("files", "photo.jpg", "image/jpeg", TestImages.TEXT))
			.with(admin()))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("File 'photo.jpg' is not a JPEG, PNG or WebP image"));
		then(imageStorage).shouldHaveNoInteractions();
	}

	@Test
	void uploadRejectsFilesOverFiveMegabytes() throws Exception {
		byte[] big = new byte[(int) AdminImageService.MAX_FILE_SIZE.toBytes() + 1];
		System.arraycopy(TestImages.PNG, 0, big, 0, TestImages.PNG.length);

		mockMvc.perform(multipart("/api/admin/products/7/images").file(file("big.png", big)).with(admin()))
			.andExpect(status().isPayloadTooLarge())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		then(imageStorage).shouldHaveNoInteractions();
	}

	@Test
	void uploadRejectsNinthImage() throws Exception {
		for (int i = 0; i < AdminImageService.MAX_IMAGES_PER_PRODUCT; i++) {
			product.addImage("https://cdn/" + i, i);
		}

		mockMvc.perform(multipart("/api/admin/products/7/images").file(file("a.png", TestImages.PNG)).with(admin()))
			.andExpect(status().isConflict());
	}

	@Test
	void uploadRequiresFilesPart() throws Exception {
		mockMvc.perform(multipart("/api/admin/products/7/images").with(admin())).andExpect(status().isBadRequest());
	}

	@Test
	void uploadReturnsNotFoundForUnknownProduct() throws Exception {
		given(productRepository.findWithDetailsById(99L)).willReturn(Optional.empty());

		mockMvc.perform(multipart("/api/admin/products/99/images").file(file("a.png", TestImages.PNG)).with(admin()))
			.andExpect(status().isNotFound());
	}

	@Test
	void reorderReturnsNewOrder() throws Exception {
		image(1L, 0);
		image(2L, 1);

		mockMvc.perform(put("/api/admin/products/7/images/order").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"imageIds\": [2, 1] }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(2))
			.andExpect(jsonPath("$[0].position").value(0))
			.andExpect(jsonPath("$[1].id").value(1));
	}

	@Test
	void reorderRejectsIncompleteList() throws Exception {
		image(1L, 0);
		image(2L, 1);

		mockMvc.perform(put("/api/admin/products/7/images/order").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"imageIds\": [2] }")).andExpect(status().isBadRequest());
		mockMvc.perform(put("/api/admin/products/7/images/order").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"imageIds\": [] }")).andExpect(status().isBadRequest());
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		ProductImage image = image(1L, 0);
		ReflectionTestUtils.setField(image, "storageKey", "key-1");
		given(imageRepository.findWithProductById(1L)).willReturn(Optional.of(image));

		mockMvc.perform(delete("/api/admin/images/1").with(admin())).andExpect(status().isNoContent());
		then(imageStorage).should().delete("key-1");
	}

	@Test
	void deleteReturnsNotFound() throws Exception {
		given(imageRepository.findWithProductById(1L)).willReturn(Optional.empty());

		mockMvc.perform(delete("/api/admin/images/1").with(admin())).andExpect(status().isNotFound());
	}

	@Test
	void requiresToken() throws Exception {
		mockMvc.perform(multipart("/api/admin/products/7/images").file(file("a.png", TestImages.PNG)))
			.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/admin/images/1")).andExpect(status().isUnauthorized());
		then(imageStorage).should(never()).store(any(), any());
	}

	private ProductImage image(Long id, int position) {
		ProductImage image = product.addImage("https://cdn/" + id, position);
		ReflectionTestUtils.setField(image, "id", id);
		return image;
	}

	private static MockMultipartFile file(String name, byte[] content) {
		return new MockMultipartFile("files", name, "application/octet-stream", content);
	}

}
