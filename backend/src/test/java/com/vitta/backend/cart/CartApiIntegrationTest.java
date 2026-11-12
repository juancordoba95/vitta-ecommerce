package com.vitta.backend.cart;

import java.math.BigDecimal;

import com.vitta.backend.TestcontainersConfiguration;
import com.vitta.backend.catalog.entity.Category;
import com.vitta.backend.catalog.entity.Product;
import com.vitta.backend.catalog.entity.ProductVariant;
import com.vitta.backend.catalog.repository.CategoryRepository;
import com.vitta.backend.catalog.repository.ProductRepository;
import com.vitta.backend.catalog.repository.ProductVariantRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test of {@code POST /api/cart/validate} against a real MySQL (Testcontainers). Each
 * test runs in a transaction that is rolled back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class CartApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductVariantRepository productVariantRepository;

	@Autowired
	private EntityManager entityManager;

	private Long small;

	private Long medium;

	private Long large;

	private Long inactive;

	@BeforeEach
	void setUp() {
		Category conjuntos = categoryRepository.save(new Category("Conjuntos", "conjuntos"));
		Product encaje = new Product("Conjunto Encaje", "conjunto-encaje", null, new BigDecimal("42900.00"), conjuntos,
				null, true);
		ProductVariant s = encaje.addVariant("S", 4);
		ProductVariant m = encaje.addVariant("M", 1);
		ProductVariant l = encaje.addVariant("L", 0);
		productRepository.save(encaje);
		Product body = new Product("Body Inactivo", "body-inactivo", null, new BigDecimal("28900.00"), conjuntos, null,
				false);
		ProductVariant hidden = body.addVariant("S", 9);
		productRepository.save(body);

		entityManager.flush();
		small = s.getId();
		medium = m.getId();
		large = l.getId();
		inactive = hidden.getId();
		entityManager.clear();
	}

	@Test
	void validatesEveryStatusAndTotalsOnlyOkItemsWithoutTouchingStock() throws Exception {
		String body = """
				{ "items": [
				  { "variantId": %d, "quantity": 2 },
				  { "variantId": %d, "quantity": 3 },
				  { "variantId": %d, "quantity": 1 },
				  { "variantId": %d, "quantity": 1 },
				  { "variantId": 999999, "quantity": 1 }
				] }
				""".formatted(small, medium, large, inactive);

		mockMvc.perform(post("/api/cart/validate").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].status").value("OK"))
			.andExpect(jsonPath("$.items[0].productSlug").value("conjunto-encaje"))
			.andExpect(jsonPath("$.items[0].size").value("S"))
			.andExpect(jsonPath("$.items[0].unitPrice").value(42900.0))
			.andExpect(jsonPath("$.items[0].availableStock").value(4))
			.andExpect(jsonPath("$.items[1].status").value("INSUFFICIENT_STOCK"))
			.andExpect(jsonPath("$.items[1].availableStock").value(1))
			.andExpect(jsonPath("$.items[2].status").value("OUT_OF_STOCK"))
			.andExpect(jsonPath("$.items[3].status").value("NOT_FOUND"))
			.andExpect(jsonPath("$.items[4].status").value("NOT_FOUND"))
			.andExpect(jsonPath("$.items[4].variantId").value(999999))
			.andExpect(jsonPath("$.total").value(85800.0));

		entityManager.clear();
		assertThat(productVariantRepository.findById(small)).get()
			.extracting(ProductVariant::getStock)
			.isEqualTo(4);
	}

	@Test
	void allowsCorsPreflightForPostFromFrontendOrigin() throws Exception {
		mockMvc.perform(options("/api/cart/validate").header(HttpHeaders.ORIGIN, "http://localhost:3000")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
	}

}
