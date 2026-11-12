package com.vitta.backend.catalog.controller;

import java.math.BigDecimal;
import java.util.List;

import com.vitta.backend.catalog.dto.CategoryResponse;
import com.vitta.backend.catalog.dto.ProductDetailResponse;
import com.vitta.backend.catalog.dto.ProductImageResponse;
import com.vitta.backend.catalog.dto.ProductVariantResponse;
import com.vitta.backend.catalog.service.ProductService;
import com.vitta.backend.common.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void returnsProductDetail() throws Exception {
		given(productService.findActiveBySlug("conjunto-encaje")).willReturn(new ProductDetailResponse(1L,
				"Conjunto Encaje", "conjunto-encaje", "Encaje negro", new BigDecimal("42900.00"),
				"https://img/front.jpg", new CategoryResponse(2L, "Conjuntos", "conjuntos"),
				List.of(new ProductImageResponse("https://img/front.jpg", 0)),
				List.of(new ProductVariantResponse(10L, "S", 4, true), new ProductVariantResponse(11L, "M", 0, false))));

		mockMvc.perform(get("/api/products/conjunto-encaje"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.slug").value("conjunto-encaje"))
			.andExpect(jsonPath("$.name").value("Conjunto Encaje"))
			.andExpect(jsonPath("$.description").value("Encaje negro"))
			.andExpect(jsonPath("$.price").value(42900.0))
			.andExpect(jsonPath("$.category.id").value(2))
			.andExpect(jsonPath("$.category.name").value("Conjuntos"))
			.andExpect(jsonPath("$.category.slug").value("conjuntos"))
			.andExpect(jsonPath("$.images", hasSize(1)))
			.andExpect(jsonPath("$.images[0].url").value("https://img/front.jpg"))
			.andExpect(jsonPath("$.images[0].position").value(0))
			.andExpect(jsonPath("$.variants", hasSize(2)))
			.andExpect(jsonPath("$.variants[0].id").value(10))
			.andExpect(jsonPath("$.variants[0].size").value("S"))
			.andExpect(jsonPath("$.variants[0].stock").value(4))
			.andExpect(jsonPath("$.variants[0].available").value(true))
			.andExpect(jsonPath("$.variants[1].available").value(false));
	}

	@Test
	void returnsProblemDetailWhenProductIsNotFound() throws Exception {
		given(productService.findActiveBySlug("missing"))
			.willThrow(new ResourceNotFoundException("Product", "missing"));

		mockMvc.perform(get("/api/products/missing"))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.title").value("Resource not found"))
			.andExpect(jsonPath("$.detail").value("Product 'missing' not found"))
			.andExpect(jsonPath("$.instance").value("/api/products/missing"));
	}

	@Test
	void returnsBadRequestForInvalidSlug() throws Exception {
		mockMvc.perform(get("/api/products/Not_A_Slug"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
		then(productService).should(never()).findActiveBySlug(anyString());
	}

}
