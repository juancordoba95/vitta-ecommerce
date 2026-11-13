package com.vitta.backend.admin.catalog.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** All image ids of the product, in the new order; the first one becomes the cover. */
public record ImageOrderRequest(@NotEmpty List<@NotNull Long> imageIds) {
}
