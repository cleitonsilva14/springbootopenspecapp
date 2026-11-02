package io.dev.openspec.springbootopenspecapp.mapper;

import io.dev.openspec.springbootopenspecapp.domain.Category;
import io.dev.openspec.springbootopenspecapp.domain.Product;
import io.dev.openspec.springbootopenspecapp.dto.CategorySummary;
import io.dev.openspec.springbootopenspecapp.dto.ProductRequest;
import io.dev.openspec.springbootopenspecapp.dto.ProductResponse;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private static Category category() {
        Category category = new Category("Eletrônicos", "d");
        category.setId(1L);
        return category;
    }

    @Test
    void mapsRequestToEntity() {
        Product product = ProductMapper.toEntity(
                new ProductRequest(" Notebook ", "desc", new BigDecimal("4500.00"), 10, 1L), category());

        assertThat(product.getId()).isNull();
        assertThat(product.getName()).isEqualTo("Notebook");
        assertThat(product.getPrice()).isEqualByComparingTo("4500.00");
        assertThat(product.getStockQuantity()).isEqualTo(10);
        assertThat(product.getCategory().getId()).isEqualTo(1L);
        assertThat(product.getCreatedAt()).isNull();
    }

    @Test
    void mapsEntityToResponseWithCategorySummary() {
        Product product = ProductMapper.toEntity(
                new ProductRequest("Notebook", null, new BigDecimal("4500.00"), 10, 1L), category());
        product.setId(5L);
        Instant now = Instant.now();
        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        ProductResponse response = ProductMapper.toResponse(product);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.category()).isEqualTo(new CategorySummary(1L, "Eletrônicos"));
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }
}
