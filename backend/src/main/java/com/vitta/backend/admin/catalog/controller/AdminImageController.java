package com.vitta.backend.admin.catalog.controller;

import java.util.List;

import com.vitta.backend.admin.catalog.dto.AdminImageResponse;
import com.vitta.backend.admin.catalog.dto.ImageOrderRequest;
import com.vitta.backend.admin.catalog.service.AdminImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminImageController {

	private final AdminImageService imageService;

	/** Multipart upload; each file goes in a {@code files} part. Returns the whole gallery. */
	@PostMapping(path = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<List<AdminImageResponse>> upload(@PathVariable Long productId,
			@RequestPart("files") List<MultipartFile> files) {
		return ResponseEntity.status(HttpStatus.CREATED).body(imageService.upload(productId, files));
	}

	@PutMapping("/products/{productId}/images/order")
	public List<AdminImageResponse> reorder(@PathVariable Long productId,
			@Valid @RequestBody ImageOrderRequest request) {
		return imageService.reorder(productId, request.imageIds());
	}

	@DeleteMapping("/images/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		imageService.delete(id);
		return ResponseEntity.noContent().build();
	}

}
