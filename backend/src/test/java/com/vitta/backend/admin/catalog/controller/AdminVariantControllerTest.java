package com.vitta.backend.admin.catalog.controller;

import com.vitta.backend.admin.catalog.dto.AdminVariantResponse;
import com.vitta.backend.admin.catalog.dto.VariantRequest;
import com.vitta.backend.admin.catalog.service.AdminVariantService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminVariantController.class)
@Import(SecurityConfig.class)
class AdminVariantControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminVariantService variantService;

	@Test
	void createReturnsCreated() throws Exception {
		given(variantService.create(5L, new VariantRequest("M", 4))).willReturn(new AdminVariantResponse(3L, "M", 4));

		mockMvc.perform(post("/api/admin/products/5/variants").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"size\": \"M\", \"stock\": 4 }"))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/api/admin/variants/3"))
			.andExpect(jsonPath("$.size").value("M"))
			.andExpect(jsonPath("$.stock").value(4));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", //
			"{ \"size\": \"\", \"stock\": 1 }", //
			"{ \"size\": \"MUY-GRANDE-XL\", \"stock\": 1 }", //
			"{ \"size\": \"M\" }", //
			"{ \"size\": \"M\", \"stock\": -1 }", //
			"{ \"size\": \"M\", \"stock\": 10000 }" })
	void createRejectsInvalidBody(String body) throws Exception {
		mockMvc.perform(post("/api/admin/products/5/variants").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
		then(variantService).should(never()).create(anyLong(), any());
	}

	@Test
	void createReturnsConflictForRepeatedSize() throws Exception {
		given(variantService.create(5L, new VariantRequest("M", 4)))
			.willThrow(new ConflictException("Size 'M' already exists for this product"));

		mockMvc.perform(post("/api/admin/products/5/variants").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"size\": \"M\", \"stock\": 4 }"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Size 'M' already exists for this product"));
	}

	@Test
	void createReturnsNotFoundForUnknownProduct() throws Exception {
		given(variantService.create(99L, new VariantRequest("M", 4)))
			.willThrow(new ResourceNotFoundException("Product", "99"));

		mockMvc.perform(post("/api/admin/products/99/variants").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"size\": \"M\", \"stock\": 4 }")).andExpect(status().isNotFound());
	}

	@Test
	void updateReturnsVariant() throws Exception {
		given(variantService.update(3L, new VariantRequest("L", 2))).willReturn(new AdminVariantResponse(3L, "L", 2));

		mockMvc.perform(put("/api/admin/variants/3").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"size\": \"L\", \"stock\": 2 }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.size").value("L"));
	}

	@Test
	void changeStockReturnsVariant() throws Exception {
		given(variantService.changeStock(3L, 9999)).willReturn(new AdminVariantResponse(3L, "L", 9999));

		mockMvc.perform(patch("/api/admin/variants/3/stock").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"stock\": 9999 }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.stock").value(9999));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{ \"stock\": -1 }", "{ \"stock\": 10000 }" })
	void changeStockRejectsOutOfRange(String body) throws Exception {
		mockMvc.perform(patch("/api/admin/variants/3/stock").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)).andExpect(status().isBadRequest());
		then(variantService).should(never()).changeStock(anyLong(), anyInt());
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/admin/variants/3").with(admin())).andExpect(status().isNoContent());
		then(variantService).should().delete(3L);
	}

	@Test
	void deleteReturnsNotFound() throws Exception {
		willThrow(new ResourceNotFoundException("Variant", "3")).given(variantService).delete(3L);

		mockMvc.perform(delete("/api/admin/variants/3").with(admin())).andExpect(status().isNotFound());
	}

	@Test
	void requiresToken() throws Exception {
		mockMvc.perform(delete("/api/admin/variants/3")).andExpect(status().isUnauthorized());
		mockMvc.perform(patch("/api/admin/variants/3/stock").contentType(MediaType.APPLICATION_JSON)
			.content("{ \"stock\": 1 }")).andExpect(status().isUnauthorized());
		then(variantService).shouldHaveNoInteractions();
	}

}
