package io.dev.openspec.springbootopenspecapp.service;

import io.dev.openspec.springbootopenspecapp.domain.Category;
import io.dev.openspec.springbootopenspecapp.dto.CategoryRequest;
import io.dev.openspec.springbootopenspecapp.dto.CategoryResponse;
import io.dev.openspec.springbootopenspecapp.exception.BusinessConflictException;
import io.dev.openspec.springbootopenspecapp.exception.ResourceNotFoundException;
import io.dev.openspec.springbootopenspecapp.repository.CategoryRepository;
import io.dev.openspec.springbootopenspecapp.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private static Category category(Long id, String name, String description) {
        Category category = new Category(name, description);
        category.setId(id);
        return category;
    }

    @Test
    void createPersistsNewCategory() {
        given(categoryRepository.existsByNameIgnoreCase("Eletrônicos")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CategoryResponse response = categoryService.create(new CategoryRequest("Eletrônicos", "Aparelhos"));

        assertThat(response).isEqualTo(new CategoryResponse(1L, "Eletrônicos", "Aparelhos"));
    }

    @Test
    void createWithDuplicatedNameThrowsConflict() {
        given(categoryRepository.existsByNameIgnoreCase("eletrônicos")).willReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("eletrônicos", null)))
                .isInstanceOf(BusinessConflictException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void findAllReturnsCategoriesSortedByName() {
        given(categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .willReturn(List.of(category(2L, "Eletrônicos", null), category(1L, "Livros", null)));

        assertThat(categoryService.findAll()).extracting(CategoryResponse::name)
                .containsExactly("Eletrônicos", "Livros");
    }

    @Test
    void findAllReturnsEmptyList() {
        given(categoryRepository.findAll(any(Sort.class))).willReturn(List.of());

        assertThat(categoryService.findAll()).isEmpty();
    }

    @Test
    void findByIdReturnsCategory() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category(1L, "Livros", "d")));

        assertThat(categoryService.findById(1L).name()).isEqualTo("Livros");
    }

    @Test
    void findByIdNotFoundThrows() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void updateChangesFields() {
        Category existing = category(1L, "Livros", "old");
        given(categoryRepository.findById(1L)).willReturn(Optional.of(existing));
        given(categoryRepository.existsByNameIgnoreCaseAndIdNot("Livros", 1L)).willReturn(false);
        given(categoryRepository.save(existing)).willReturn(existing);

        CategoryResponse response = categoryService.update(1L, new CategoryRequest("Livros", "new"));

        assertThat(response.description()).isEqualTo("new");
    }

    @Test
    void updateToNameOfAnotherCategoryThrowsConflict() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category(1L, "Livros", null)));
        given(categoryRepository.existsByNameIgnoreCaseAndIdNot("Eletrônicos", 1L)).willReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequest("Eletrônicos", null)))
                .isInstanceOf(BusinessConflictException.class);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updateNotFoundThrows() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(999L, new CategoryRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteWithoutProductsRemovesCategory() {
        Category existing = category(1L, "Livros", null);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(existing));
        given(productRepository.existsByCategoryId(1L)).willReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(existing);
    }

    @Test
    void deleteWithProductsThrowsConflict() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category(1L, "Livros", null)));
        given(productRepository.existsByCategoryId(1L)).willReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L)).isInstanceOf(BusinessConflictException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteNotFoundThrows() {
        given(categoryRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(999L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
