package io.dev.openspec.springbootopenspecapp.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must have at most 150 characters")
        String name,

        @Size(max = 1000, message = "description must have at most 1000 characters")
        String description,

        @NotNull(message = "price is required")
        @Positive(message = "price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "price must have at most 10 integer digits and 2 decimals")
        BigDecimal price,

        @NotNull(message = "stockQuantity is required")
        @PositiveOrZero(message = "stockQuantity must be zero or greater")
        Integer stockQuantity,

        @NotNull(message = "categoryId is required")
        Long categoryId
) {
}
