package io.dev.openspec.springbootopenspecapp.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        CategorySummary category,
        Instant createdAt,
        Instant updatedAt
) {
}
