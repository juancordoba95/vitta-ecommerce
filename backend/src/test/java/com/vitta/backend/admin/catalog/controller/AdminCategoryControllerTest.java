package com.vitta.backend.admin.catalog.controller;

import java.util.List;

import com.vitta.backend.admin.catalog.dto.CategoryRequest;
import com.vitta.backend.admin.catalog.service.AdminCategoryService;
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
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCategoryController.class)
@Import(SecurityConfig.class)
class AdminCategoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminCategoryService categoryService;

	@Test
	void findAllReturnsCategories() throws Exception {
		given(categoryService.findAll()).willReturn(List.of(new CategoryResponse(1L, "Bodies", "bodies")));

		mockMvc.perform(get("/api/admin/categories").with(admin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].slug").value("bodies"));
	}

	@Test
	void createReturnsCreated() throws Exception {
		given(categoryService.create(new CategoryRequest("Medias", null)))
			.willReturn(new CategoryResponse(4L, "Medias", "medias"));

		mockMvc.perform(post("/api/admin/categories").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"name\": \"Medias\" }"))
			.andExpect(status().isCreated())
			.andExpect(header().string(HttpHeaders.LOCATION, "/api/admin/categories/4"))
			.andExpect(jsonPath("$.slug").value("medias"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{ \"name\": \" \" }", "{ \"name\": \"Medias\", \"slug\": \"-medias\" }" })
	void createRejectsInvalidBody(String body) throws Exception {
		mockMvc.perform(post("/api/admin/categories").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest());
		then(categoryService).should(never()).create(any());
	}

	@Test
	void createReturnsConflictForDuplicateSlug() throws Exception {
		given(categoryService.create(new CategoryRequest("Bodies", "bodies")))
			.willThrow(new ConflictException("Category slug 'bodies' is already in use"));

		mockMvc.perform(post("/api/admin/categories").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"name\": \"Bodies\", \"slug\": \"bodies\" }")).andExpect(status().isConflict());
	}

	@Test
	void updateReturnsCategory() throws Exception {
		given(categoryService.update(1L, new CategoryRequest("Bodies", "")))
			.willReturn(new CategoryResponse(1L, "Bodies", "bodies"));

		mockMvc.perform(put("/api/admin/categories/1").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"name\": \"Bodies\", \"slug\": \"\" }"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Bodies"));
	}

	@Test
	void updateReturnsNotFound() throws Exception {
		given(categoryService.update(9L, new CategoryRequest("X", null)))
			.willThrow(new ResourceNotFoundException("Category", "9"));

		mockMvc.perform(put("/api/admin/categories/9").with(admin())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"name\": \"X\" }")).andExpect(status().isNotFound());
	}

	@Test
	void hasNoDelete() throws Exception {
		mockMvc.perform(delete("/api/admin/categories/1").with(admin())).andExpect(status().isMethodNotAllowed());
	}

	@Test
	void requiresToken() throws Exception {
		mockMvc.perform(get("/api/admin/categories")).andExpect(status().isUnauthorized());
		then(categoryService).shouldHaveNoInteractions();
	}

}
