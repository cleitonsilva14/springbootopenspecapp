package io.dev.openspec.springbootopenspecapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must have at most 100 characters")
        String name,

        @Size(max = 500, message = "description must have at most 500 characters")
        String description
) {
}
