package io.dev.openspec.springbootopenspecapp.mapper;

import io.dev.openspec.springbootopenspecapp.domain.Category;
import io.dev.openspec.springbootopenspecapp.dto.CategoryRequest;
import io.dev.openspec.springbootopenspecapp.dto.CategoryResponse;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category toEntity(CategoryRequest request) {
        return new Category(request.name().trim(), request.description());
    }

    public static void updateEntity(Category category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(request.description());
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
