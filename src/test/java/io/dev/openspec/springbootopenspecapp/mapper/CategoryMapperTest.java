package io.dev.openspec.springbootopenspecapp.mapper;

import io.dev.openspec.springbootopenspecapp.domain.Category;
import io.dev.openspec.springbootopenspecapp.dto.CategoryRequest;
import io.dev.openspec.springbootopenspecapp.dto.CategoryResponse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    @Test
    void mapsRequestToEntityTrimmingName() {
        Category category = CategoryMapper.toEntity(new CategoryRequest("  Livros ", "Papel"));

        assertThat(category.getId()).isNull();
        assertThat(category.getName()).isEqualTo("Livros");
        assertThat(category.getDescription()).isEqualTo("Papel");
    }

    @Test
    void updatesEntityFromRequest() {
        Category category = new Category("Old", "old");
        CategoryMapper.updateEntity(category, new CategoryRequest("New", "new"));

        assertThat(category.getName()).isEqualTo("New");
        assertThat(category.getDescription()).isEqualTo("new");
    }

    @Test
    void mapsEntityToResponse() {
        Category category = new Category("Livros", "Papel");
        category.setId(7L);

        assertThat(CategoryMapper.toResponse(category)).isEqualTo(new CategoryResponse(7L, "Livros", "Papel"));
    }
}
