package io.dev.openspec.springbootopenspecapp.controller;

import io.dev.openspec.springbootopenspecapp.common.dto.PageResponse;
import io.dev.openspec.springbootopenspecapp.dto.CategorySummary;
import io.dev.openspec.springbootopenspecapp.dto.ProductRequest;
import io.dev.openspec.springbootopenspecapp.dto.ProductResponse;
import io.dev.openspec.springbootopenspecapp.exception.ResourceNotFoundException;
import io.dev.openspec.springbootopenspecapp.security.WebSecurityTestImports;
import io.dev.openspec.springbootopenspecapp.service.ProductService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@WebSecurityTestImports
class ProductControllerTest {

    private static final String VALID_BODY =
            "{\"name\":\"Notebook\",\"price\":4500.00,\"stockQuantity\":10,\"categoryId\":1}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static ProductResponse response(Long id, Long categoryId) {
        Instant now = Instant.parse("2026-11-02T10:00:00Z");
        return new ProductResponse(id, "Notebook", null, new BigDecimal("4500.00"), 10,
                new CategorySummary(categoryId, "Eletrônicos"), now, now);
    }

    @Test
    @WithMockUser
    void createReturns201WithLocationAndCategorySummary() throws Exception {
        given(productService.create(any(ProductRequest.class))).willReturn(response(5L, 1L));

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/products/5")))
                .andExpect(jsonPath("$.category.id").value(1))
                .andExpect(jsonPath("$.category.name").value("Eletrônicos"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-02T10:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-11-02T10:00:00Z"));
    }

    @Test
    @WithMockUser
    void createWithMultipleInvalidFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1,\"stockQuantity\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        containsInAnyOrder("name", "price", "stockQuantity", "categoryId")));
        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser
    void createWithZeroPriceReturns400() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"price\":0,\"stockQuantity\":0,\"categoryId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
    }

    @Test
    @WithMockUser
    void createWithMissingCategoryReturns404() throws Exception {
        given(productService.create(any())).willThrow(new ResourceNotFoundException("Category", 999L));

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"price\":1,\"stockQuantity\":0,\"categoryId\":999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category with id 999 not found"));
    }

    @Test
    @WithMockUser
    void findAllUsesDefaultPagination() throws Exception {
        given(productService.findAll(isNull(), any(Pageable.class)))
                .willReturn(new PageResponse<>(List.of(response(1L, 1L)), 0, 20, 25, 2));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).findAll(isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNotNull();
    }

    @Test
    @WithMockUser
    void findAllPassesPageAndCategoryFilter() throws Exception {
        given(productService.findAll(eq(1L), any(Pageable.class)))
                .willReturn(new PageResponse<>(List.of(), 1, 20, 3, 1));

        mockMvc.perform(get("/api/products").param("categoryId", "1").param("page", "1").param("size", "20"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).findAll(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
    }

    @Test
    @WithMockUser
    void findAllWithMissingCategoryReturns404() throws Exception {
        given(productService.findAll(eq(999L), any(Pageable.class)))
                .willThrow(new ResourceNotFoundException("Category", 999L));

        mockMvc.perform(get("/api/products").param("categoryId", "999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void findByIdReturns200() throws Exception {
        given(productService.findById(5L)).willReturn(response(5L, 1L));

        mockMvc.perform(get("/api/products/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    @WithMockUser
    void findByIdNotFoundReturns404() throws Exception {
        given(productService.findById(999L)).willThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(get("/api/products/999")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void invalidPathVariableReturns400() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser
    void updateReturns200() throws Exception {
        given(productService.update(eq(5L), any())).willReturn(response(5L, 2L));

        mockMvc.perform(put("/api/products/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Notebook\",\"price\":4500.00,\"stockQuantity\":10,\"categoryId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category.id").value(2));
    }

    @Test
    @WithMockUser
    void updateNotFoundReturns404() throws Exception {
        given(productService.update(eq(999L), any())).willThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(put("/api/products/999").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/products/5")).andExpect(status().isNoContent());
        verify(productService).delete(5L);
    }

    @Test
    void requestWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/products"));
        verifyNoInteractions(productService);
    }
}
