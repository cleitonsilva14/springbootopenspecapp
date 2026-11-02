package io.dev.openspec.springbootopenspecapp.service;

import io.dev.openspec.springbootopenspecapp.domain.Category;
import io.dev.openspec.springbootopenspecapp.dto.CategoryRequest;
import io.dev.openspec.springbootopenspecapp.dto.CategoryResponse;
import io.dev.openspec.springbootopenspecapp.exception.BusinessConflictException;
import io.dev.openspec.springbootopenspecapp.exception.ResourceNotFoundException;
import io.dev.openspec.springbootopenspecapp.mapper.CategoryMapper;
import io.dev.openspec.springbootopenspecapp.repository.CategoryRepository;
import io.dev.openspec.springbootopenspecapp.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    static final String RESOURCE = "Category";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw duplicatedName(name);
        }
        return CategoryMapper.toResponse(categoryRepository.save(CategoryMapper.toEntity(request)));
    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    public CategoryResponse findById(Long id) {
        return CategoryMapper.toResponse(getCategory(id));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicatedName(name);
        }
        CategoryMapper.updateEntity(category, request);
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessConflictException("Category with id " + id + " has associated products and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private BusinessConflictException duplicatedName(String name) {
        return new BusinessConflictException("Category with name '" + name + "' already exists");
    }
}
