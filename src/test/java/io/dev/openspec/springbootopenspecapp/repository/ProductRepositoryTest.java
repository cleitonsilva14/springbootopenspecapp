package io.dev.openspec.springbootopenspecapp.repository;

import io.dev.openspec.springbootopenspecapp.domain.Category;
import io.dev.openspec.springbootopenspecapp.domain.Product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category books;

    @BeforeEach
    void setUp() {
        electronics = categoryRepository.save(new Category("Eletrônicos", null));
        books = categoryRepository.save(new Category("Livros", null));
    }

    private Product product(String name, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQuantity(1);
        product.setCategory(category);
        return product;
    }

    @Test
    void filtersByCategory() {
        for (int i = 0; i < 3; i++) {
            productRepository.save(product("E" + i, electronics));
        }
        productRepository.save(product("B0", books));
        productRepository.save(product("B1", books));

        Page<Product> page = productRepository.findByCategoryId(electronics.getId(), PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).allMatch(p -> p.getCategory().getId().equals(electronics.getId()));
    }

    @Test
    void paginatesResults() {
        for (int i = 0; i < 25; i++) {
            productRepository.save(product(String.format("P%02d", i), electronics));
        }

        Page<Product> first = productRepository.findAll(PageRequest.of(0, 20, Sort.by("name")));
        Page<Product> second = productRepository.findAll(PageRequest.of(1, 20, Sort.by("name")));

        assertThat(first.getContent()).hasSize(20);
        assertThat(first.getTotalElements()).isEqualTo(25);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(second.getContent()).hasSize(5);
    }

    @Test
    void existsByCategoryId() {
        productRepository.save(product("E", electronics));

        assertThat(productRepository.existsByCategoryId(electronics.getId())).isTrue();
        assertThat(productRepository.existsByCategoryId(books.getId())).isFalse();
    }

    @Test
    void fillsAndUpdatesTimestamps() throws InterruptedException {
        Product saved = productRepository.saveAndFlush(product("Notebook", electronics));
        Instant createdAt = saved.getCreatedAt();
        Instant firstUpdatedAt = saved.getUpdatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(firstUpdatedAt).isNotNull();

        Thread.sleep(20);
        saved.setStockQuantity(5);
        Product updated = productRepository.saveAndFlush(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(firstUpdatedAt);
    }
}
