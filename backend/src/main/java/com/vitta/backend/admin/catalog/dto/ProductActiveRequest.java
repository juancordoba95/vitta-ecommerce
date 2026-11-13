package com.vitta.backend.admin.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record ProductActiveRequest(@NotNull Boolean active) {
}
