package io.dev.openspec.springbootopenspecapp.mapper;

import io.dev.openspec.springbootopenspecapp.domain.Category;
import io.dev.openspec.springbootopenspecapp.domain.Product;
import io.dev.openspec.springbootopenspecapp.dto.CategorySummary;
import io.dev.openspec.springbootopenspecapp.dto.ProductRequest;
import io.dev.openspec.springbootopenspecapp.dto.ProductResponse;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static Product toEntity(ProductRequest request, Category category) {
        Product product = new Product();
        updateEntity(product, request, category);
        return product;
    }

    public static void updateEntity(Product product, ProductRequest request, Category category) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(category);
    }

    public static ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                new CategorySummary(category.getId(), category.getName()),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
