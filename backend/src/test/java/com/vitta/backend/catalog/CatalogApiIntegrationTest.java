package com.vitta.backend.catalog;

import java.math.BigDecimal;

import com.vitta.backend.TestcontainersConfiguration;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.repository.CategoryRepository;
import com.vitta.backend.catalog.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of the catalog endpoints against a real MySQL (Testcontainers). Each test
 * runs in a transaction that is rolled back, so the data below is isolated per test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CatalogApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		Category conjuntos = categoryRepository.save(new Category("Conjuntos", "conjuntos"));
		Category bodies = categoryRepository.save(new Category("Bodies", "bodies"));

		Product encaje = new Product("Conjunto Encaje Floral", "conjunto-encaje-floral", "Encaje negro",
				new BigDecimal("42900.00"), conjuntos, "https://placehold.co/600x800", true);
		encaje.addVariant("S", 4);
		encaje.addVariant("M", 0);
		encaje.addVariant("L", 3);
		productRepository.save(encaje);
		productRepository.save(new Product("Body Encaje Negro", "body-encaje-negro", "Body de encaje",
				new BigDecimal("45900.00"), bodies, "https://placehold.co/600x800", true));
		productRepository.save(new Product("Body Lycra Basico", "body-lycra-basico", "Inactivo",
				new BigDecimal("28900.00"), bodies, "https://placehold.co/600x800", false));

		// Force reads to hit the database instead of the persistence context.
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void listsCategoriesSortedByName() throws Exception {
		mockMvc.perform(get("/api/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].name").value("Bodies"))
			.andExpect(jsonPath("$[0].slug").value("bodies"))
			.andExpect(jsonPath("$[1].slug").value("conjuntos"));
	}

	@Test
	void listsOnlyActiveProducts() throws Exception {
		mockMvc.perform(get("/api/products"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].slug").value("body-encaje-negro"))
			.andExpect(jsonPath("$[0].categorySlug").value("bodies"))
			.andExpect(jsonPath("$[1].slug").value("conjunto-encaje-floral"))
			.andExpect(jsonPath("$[1].price").value(42900.0));
	}

	@Test
	void filtersProductsByCategory() throws Exception {
		mockMvc.perform(get("/api/products").param("category", "bodies"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].slug").value("body-encaje-negro"));
	}

	@Test
	void returnsNotFoundForUnknownCategory() throws Exception {
		mockMvc.perform(get("/api/products").param("category", "unknown"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Category 'unknown' not found"));
	}

	@Test
	void returnsProductDetailWithVariants() throws Exception {
		mockMvc.perform(get("/api/products/conjunto-encaje-floral"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Conjunto Encaje Floral"))
			.andExpect(jsonPath("$.description").value("Encaje negro"))
			.andExpect(jsonPath("$.category.slug").value("conjuntos"))
			.andExpect(jsonPath("$.variants", hasSize(3)))
			.andExpect(jsonPath("$.variants[0].size").value("S"))
			.andExpect(jsonPath("$.variants[0].stock").value(4))
			.andExpect(jsonPath("$.variants[1].stock").value(0));
	}

	@Test
	void returnsProblemDetailWhenProductDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/products/does-not-exist"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.title").value("Resource not found"))
			.andExpect(jsonPath("$.detail").value("Product 'does-not-exist' not found"))
			.andExpect(jsonPath("$.instance").value("/api/products/does-not-exist"));
	}

	@Test
	void returnsNotFoundForInactiveProduct() throws Exception {
		mockMvc.perform(get("/api/products/body-lycra-basico")).andExpect(status().isNotFound());
	}

	@Test
	void returnsBadRequestForInvalidSlug() throws Exception {
		mockMvc.perform(get("/api/products/Not_A_Slug"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void allowsCorsFromConfiguredFrontendOrigin() throws Exception {
		mockMvc.perform(get("/api/categories").header(HttpHeaders.ORIGIN, "http://localhost:3000"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
	}

	@Test
	void rejectsCorsFromOtherOrigins() throws Exception {
		mockMvc.perform(get("/api/categories").header(HttpHeaders.ORIGIN, "https://evil.example"))
			.andExpect(status().isForbidden());
	}

}
