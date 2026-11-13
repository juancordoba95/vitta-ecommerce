package com.vitta.backend.admin.catalog.controller;

import java.math.BigDecimal;
import java.util.List;

import com.vitta.backend.admin.catalog.dto.AdminImageResponse;
import com.vitta.backend.admin.catalog.dto.AdminProductDetailResponse;
import com.vitta.backend.admin.catalog.dto.AdminProductSummaryResponse;
import com.vitta.backend.admin.catalog.dto.AdminVariantResponse;
import com.vitta.backend.admin.catalog.dto.PageResponse;
import com.vitta.backend.admin.catalog.dto.ProductRequest;
import com.vitta.backend.admin.catalog.service.AdminProductService;
import com.vitta.backend.catalog.dto.CategoryResponse;
import com.vitta.backend.common.error.ConflictException;
import com.vitta.backend.common.error.ResourceNotFoundException;
import com.vitta.backend.common.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.vitta.backend.admin.AdminTestAuth.admin;
import static com.vitta.backend.admin.AdminTestAuth.nonAdmin;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductController.class)
@Import(SecurityConfig.class)
class AdminProductControllerTest {

	private static final String VALID_BODY = """
			{ "name": "Conjunto Encaje", "description": "Negro", "price": 42900.00, "categoryId": 1, "slug": "" }
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminProductService productService;

	@Test
	void searchReturnsPageWithFilters() throws Exception {
		given(productService.search("encaje", 1L, 2, 10)).willReturn(new PageResponse<>(
				List.of(new AdminProductSummaryResponse(5L, "Conjunto Encaje", "conjunto-encaje",
						new BigDecimal("42900.00"), null, category(), false, null)),
				2, 10, 21, 3));

		mockMvc.perform(get("/api/admin/products?q=encaje&categoryId=1&page=2&size=10").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.content[0].active").value(false))
			.andExpect(jsonPath("$.content[0].category.slug").value("conjuntos"))
			.andExpect(jsonPath("$.page").value(2))
			.andExpect(jsonPath("$.totalElements").value(21))
			.andExpect(jsonPath("$.totalPages").value(3));
	}

	@Test
	void searchUsesDefaultPaging() throws Exception {
		given(productService.search(null, null, 0, 20)).willReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/admin/products").with(admin())).andExpect(status().isOk());
	}

	@ParameterizedTest
	@ValueSource(strings = { "page=-1", "size=0", "size=101", "categoryId=0", "page=abc" })
	void searchRejectsInvalidPaging(String query) throws Exception {
		mockMvc.perform(get("/api/admin/products?" + query).with(admin()))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		then(productService).should(never()).search(any(), any(), anyInt(), anyInt());
	}

	@Test
	void findByIdReturnsDetailWithVariantsAndImages() throws Exception {
		given(productService.findById(5L)).willReturn(detail());

		mockMvc.perform(get("/api/admin/products/5").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(5))
			.andExpect(jsonPath("$.variants[0].size").value("S"))
			.andExpect(jsonPath("$.images[0].id").value(9))
			.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void findByIdReturnsNotFound() throws Exception {
		given(productService.findById(99L)).willThrow(new ResourceNotFoundException("Product", "99"));

		mockMvc.perform(get("/api/admin/products/99").with(admin()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("Product '99' not found"));
	}

	@Test
	void createReturnsCreatedWithLocation() throws Exception {
		given(productService.create(any(ProductRequest.class))).willReturn(detail());

		mockMvc.perform(post("/api/admin/products").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/api/admin/products/5"))
			.andExpect(jsonPath("$.slug").value("conjunto-encaje"));
		then(productService).should()
			.create(new ProductRequest("Conjunto Encaje", "Negro", new BigDecimal("42900.00"), 1L, ""));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", //
			"{ \"name\": \"\", \"price\": 10, \"categoryId\": 1 }", //
			"{ \"name\": \"X\", \"price\": 0, \"categoryId\": 1 }", //
			"{ \"name\": \"X\", \"price\": -5, \"categoryId\": 1 }", //
			"{ \"name\": \"X\", \"price\": 10.999, \"categoryId\": 1 }", //
			"{ \"name\": \"X\", \"price\": 10 }", //
			"{ \"name\": \"X\", \"price\": 10, \"categoryId\": 1, \"slug\": \"Con Espacios\" }", //
			"not json" })
	void createRejectsInvalidBody(String body) throws Exception {
		mockMvc.perform(post("/api/admin/products").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
		then(productService).should(never()).create(any());
	}

	@Test
	void createReturnsConflictForDuplicateSlug() throws Exception {
		given(productService.create(any(ProductRequest.class)))
			.willThrow(new ConflictException("Product slug 'conjunto' is already in use"));

		mockMvc.perform(post("/api/admin/products").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Conflict"));
	}

	@Test
	void updateReturnsDetail() throws Exception {
		given(productService.update(eq(5L), any(ProductRequest.class))).willReturn(detail());

		mockMvc.perform(put("/api/admin/products/5").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content(VALID_BODY)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(5));
	}

	@Test
	void setActiveReturnsDetail() throws Exception {
		given(productService.setActive(5L, true)).willReturn(detail());

		mockMvc.perform(patch("/api/admin/products/5/active").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"active\": true }")).andExpect(status().isOk());
	}

	@Test
	void setActiveRequiresFlag() throws Exception {
		mockMvc.perform(patch("/api/admin/products/5/active").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).andExpect(status().isBadRequest());
	}

	@Test
	void requiresToken() throws Exception {
		mockMvc.perform(get("/api/admin/products"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		mockMvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
			.andExpect(status().isUnauthorized());
		then(productService).shouldHaveNoInteractions();
	}

	@Test
	void requiresAdminRole() throws Exception {
		mockMvc.perform(get("/api/admin/products").with(nonAdmin())).andExpect(status().isForbidden());
	}

	private static CategoryResponse category() {
		return new CategoryResponse(1L, "Conjuntos", "conjuntos");
	}

	private static AdminProductDetailResponse detail() {
		return new AdminProductDetailResponse(5L, "Conjunto Encaje", "conjunto-encaje", "Negro",
				new BigDecimal("42900.00"), "https://cdn/1.png", false, category(),
				List.of(new AdminImageResponse(9L, "https://cdn/1.png", 0)),
				List.of(new AdminVariantResponse(3L, "S", 4)), null, null);
	}

}
