package io.dev.openspec.springbootopenspecapp.service;

import io.dev.openspec.springbootopenspecapp.common.dto.PageResponse;
import io.dev.openspec.springbootopenspecapp.domain.Category;
import io.dev.openspec.springbootopenspecapp.domain.Product;
import io.dev.openspec.springbootopenspecapp.dto.ProductRequest;
import io.dev.openspec.springbootopenspecapp.dto.ProductResponse;
import io.dev.openspec.springbootopenspecapp.exception.ResourceNotFoundException;
import io.dev.openspec.springbootopenspecapp.mapper.ProductMapper;
import io.dev.openspec.springbootopenspecapp.repository.CategoryRepository;
import io.dev.openspec.springbootopenspecapp.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = getCategory(request.categoryId());
        Product saved = productRepository.saveAndFlush(ProductMapper.toEntity(request, category));
        return ProductMapper.toResponse(saved);
    }

    public PageResponse<ProductResponse> findAll(Long categoryId, Pageable pageable) {
        Page<Product> page;
        if (categoryId == null) {
            page = productRepository.findAll(pageable);
        } else {
            if (!categoryRepository.existsById(categoryId)) {
                throw new ResourceNotFoundException("Category", categoryId);
            }
            page = productRepository.findByCategoryId(categoryId, pageable);
        }
        return PageResponse.from(page.map(ProductMapper::toResponse));
    }

    public ProductResponse findById(Long id) {
        return ProductMapper.toResponse(getProduct(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        Category category = getCategory(request.categoryId());
        ProductMapper.updateEntity(product, request, category);
        return ProductMapper.toResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(getProduct(id));
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }
}
