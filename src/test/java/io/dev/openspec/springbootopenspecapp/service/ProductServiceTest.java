package io.dev.openspec.springbootopenspecapp.service;

import io.dev.openspec.springbootopenspecapp.common.dto.PageResponse;
import io.dev.openspec.springbootopenspecapp.domain.Category;
import io.dev.openspec.springbootopenspecapp.domain.Product;
import io.dev.openspec.springbootopenspecapp.dto.ProductRequest;
import io.dev.openspec.springbootopenspecapp.dto.ProductResponse;
import io.dev.openspec.springbootopenspecapp.exception.ResourceNotFoundException;
import io.dev.openspec.springbootopenspecapp.repository.CategoryRepository;
import io.dev.openspec.springbootopenspecapp.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private final Pageable pageable = PageRequest.of(0, 20);

    private static Category category(Long id, String name) {
        Category category = new Category(name, null);
        category.setId(id);
        return category;
    }

    private static Product product(Long id, Category category) {
        Product product = new Product();
        product.setId(id);
        product.setName("Notebook");
        product.setPrice(new BigDecimal("4500.00"));
        product.setStockQuantity(10);
        product.setCategory(category);
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        return product;
    }

    private static ProductRequest request(Long categoryId) {
        return new ProductRequest("Notebook", null, new BigDecimal("4500.00"), 10, categoryId);
    }

    @Test
    void createWithExistingCategoryPersistsProduct() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category(1L, "Eletrônicos")));
        given(productRepository.saveAndFlush(any(Product.class))).willAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(5L);
            return p;
        });

        ProductResponse response = productService.create(request(1L));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.category().name()).isEqualTo("Eletrônicos");
    }

    @Test
    void createWithMissingCategoryThrowsAndDoesNotPersist() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request(999L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category with id 999 not found");
        verify(productRepository, never()).save(any());
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void findAllWithoutFilterReturnsPage() {
        Category category = category(1L, "Eletrônicos");
        given(productRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(product(1L, category), product(2L, category)), pageable, 2));

        PageResponse<ProductResponse> page = productService.findAll(null, pageable);

        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void findAllFilteredByExistingCategory() {
        Category category = category(1L, "Eletrônicos");
        given(categoryRepository.existsById(1L)).willReturn(true);
        given(productRepository.findByCategoryId(1L, pageable))
                .willReturn(new PageImpl<>(List.of(product(1L, category)), pageable, 1));

        PageResponse<ProductResponse> page = productService.findAll(1L, pageable);

        assertThat(page.content()).extracting(p -> p.category().id()).containsOnly(1L);
        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findAllFilteredByMissingCategoryThrows() {
        given(categoryRepository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> productService.findAll(999L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByIdReturnsProduct() {
        given(productRepository.findById(5L)).willReturn(Optional.of(product(5L, category(1L, "E"))));

        assertThat(productService.findById(5L).id()).isEqualTo(5L);
    }

    @Test
    void findByIdNotFoundThrows() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product with id 999 not found");
    }

    @Test
    void updateChangesCategory() {
        Product existing = product(5L, category(1L, "Eletrônicos"));
        given(productRepository.findById(5L)).willReturn(Optional.of(existing));
        given(categoryRepository.findById(2L)).willReturn(Optional.of(category(2L, "Livros")));
        given(productRepository.saveAndFlush(existing)).willReturn(existing);

        ProductResponse response = productService.update(5L, request(2L));

        assertThat(response.category().id()).isEqualTo(2L);
    }

    @Test
    void updateWithMissingCategoryThrowsAndKeepsProduct() {
        Product existing = product(5L, category(1L, "Eletrônicos"));
        given(productRepository.findById(5L)).willReturn(Optional.of(existing));
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(5L, request(999L)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(existing.getCategory().getId()).isEqualTo(1L);
        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateNotFoundThrows() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(999L, request(1L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesProduct() {
        Product existing = product(5L, category(1L, "E"));
        given(productRepository.findById(5L)).willReturn(Optional.of(existing));

        productService.delete(5L);

        verify(productRepository).delete(existing);
    }

    @Test
    void deleteNotFoundThrows() {
        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> productService.delete(999L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
