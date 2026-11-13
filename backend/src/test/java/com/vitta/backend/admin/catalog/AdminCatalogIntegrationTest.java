package com.vitta.backend.admin.catalog;

import java.nio.file.Path;

import com.jayway.jsonpath.JsonPath;
import com.vitta.backend.TestcontainersConfiguration;
import com.vitta.backend.storage.TestImages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static com.vitta.backend.admin.AdminTestAuth.admin;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Admin catalog flow against a real MySQL (Testcontainers) and local image storage in a temp
 * directory: unique constraints, the {@code storage_key} migration and image renumbering run
 * for real. Each test is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AdminCatalogIntegrationTest {

	@TempDir
	static Path uploadsDir;

	@DynamicPropertySource
	static void storageProperties(DynamicPropertyRegistry registry) {
		registry.add("app.storage.cloudinary-url", () -> "");
		registry.add("app.storage.uploads-dir", () -> uploadsDir.toString());
	}

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createdProductBecomesPublicOnceActivated() throws Exception {
		long categoryId = id(postJson("/api/admin/categories", "{ \"name\": \"Lencería Fina\" }")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value("lenceria-fina")));

		long productId = id(postJson("/api/admin/products", """
				{ "name": "Corpiño Señorita", "description": "Encaje", "price": 25900.50, "categoryId": %d }
				""".formatted(categoryId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.slug").value("corpino-senorita"))
			.andExpect(jsonPath("$.active").value(false)));

		postJson("/api/admin/products/" + productId + "/variants", "{ \"size\": \"m\", \"stock\": 3 }")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.size").value("M"));
		postJson("/api/admin/products/" + productId + "/variants", "{ \"size\": \"M\", \"stock\": 1 }")
			.andExpect(status().isConflict());

		// Inactive: hidden from the public catalog.
		mockMvc.perform(get("/api/products/corpino-senorita")).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/products?category=lenceria-fina")).andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(patch("/api/admin/products/" + productId + "/active").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"active\": true }")).andExpect(status().isOk());

		mockMvc.perform(get("/api/products?category=lenceria-fina"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].slug").value("corpino-senorita"));
		mockMvc.perform(get("/api/products/corpino-senorita"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.variants[0].size").value("M"))
			.andExpect(jsonPath("$.variants[0].stock").value(3));

		// Same name again: the generated slug gets a suffix; an explicit duplicate is a conflict.
		postJson("/api/admin/products", """
				{ "name": "Corpiño Señorita", "price": 1, "categoryId": %d }
				""".formatted(categoryId)).andExpect(jsonPath("$.slug").value("corpino-senorita-2"));
		postJson("/api/admin/products", """
				{ "name": "Otro", "price": 1, "categoryId": %d, "slug": "corpino-senorita" }
				""".formatted(categoryId)).andExpect(status().isConflict());

		mockMvc.perform(get("/api/admin/products?q=SEÑORITA&categoryId=" + categoryId).with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void imagesCanBeUploadedReorderedServedAndDeleted() throws Exception {
		long categoryId = id(postJson("/api/admin/categories", "{ \"name\": \"Bodies Test\" }"));
		long productId = id(postJson("/api/admin/products", """
				{ "name": "Body Imagenes", "price": 100, "categoryId": %d }
				""".formatted(categoryId)));

		String uploaded = mockMvc
			.perform(multipart("/api/admin/products/" + productId + "/images")
				.file(new MockMultipartFile("files", "a.png", "image/png", TestImages.PNG))
				.file(new MockMultipartFile("files", "b.webp", "image/webp", TestImages.WEBP))
				.with(admin()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$", hasSize(2)))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long first = ((Number) JsonPath.read(uploaded, "$[0].id")).longValue();
		long second = ((Number) JsonPath.read(uploaded, "$[1].id")).longValue();
		String firstUrl = JsonPath.read(uploaded, "$[0].url");

		// Local storage serves the file publicly.
		mockMvc.perform(get(firstUrl.substring(firstUrl.indexOf("/uploads/"))))
			.andExpect(status().isOk())
			.andExpect(content().bytes(TestImages.PNG));

		mockMvc.perform(put("/api/admin/products/" + productId + "/images/order").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"imageIds\": [%d, %d] }".formatted(second, first)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(second))
			.andExpect(jsonPath("$[0].position").value(0));

		mockMvc.perform(delete("/api/admin/images/" + second).with(admin())).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/admin/products/" + productId).with(admin()))
			.andExpect(jsonPath("$.images", hasSize(1)))
			.andExpect(jsonPath("$.images[0].id").value(first))
			.andExpect(jsonPath("$.images[0].position").value(0))
			.andExpect(jsonPath("$.imageUrl").value(firstUrl));
	}

	private ResultActions postJson(String path, String body) throws Exception {
		return mockMvc.perform(post(path).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private static long id(ResultActions result) throws Exception {
		return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

}
