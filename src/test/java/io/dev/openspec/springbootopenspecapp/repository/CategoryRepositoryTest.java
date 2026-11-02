package io.dev.openspec.springbootopenspecapp.repository;

import io.dev.openspec.springbootopenspecapp.domain.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void existsByNameIgnoresCase() {
        categoryRepository.save(new Category("Eletrônicos", null));

        assertThat(categoryRepository.existsByNameIgnoreCase("ELETRÔNICOS")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("Livros")).isFalse();
    }

    @Test
    void existsByNameIgnoringOwnId() {
        Category saved = categoryRepository.save(new Category("Livros", null));

        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("livros", saved.getId())).isFalse();
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("livros", saved.getId() + 1)).isTrue();
    }

    @Test
    void uniqueConstraintRejectsDuplicatedName() {
        categoryRepository.saveAndFlush(new Category("Games", null));

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(new Category("Games", "dup")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
