package io.dev.openspec.springbootopenspecapp.controller;

import io.dev.openspec.springbootopenspecapp.dto.CategoryRequest;
import io.dev.openspec.springbootopenspecapp.dto.CategoryResponse;
import io.dev.openspec.springbootopenspecapp.exception.BusinessConflictException;
import io.dev.openspec.springbootopenspecapp.exception.ResourceNotFoundException;
import io.dev.openspec.springbootopenspecapp.security.WebSecurityTestImports;
import io.dev.openspec.springbootopenspecapp.service.CategoryService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@WebSecurityTestImports
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    @WithMockUser
    void createReturns201WithLocation() throws Exception {
        given(categoryService.create(any(CategoryRequest.class)))
                .willReturn(new CategoryResponse(1L, "Eletrônicos", "Aparelhos"));

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eletrônicos\",\"description\":\"Aparelhos\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/categories/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Eletrônicos"))
                .andExpect(jsonPath("$.description").value("Aparelhos"));
    }

    @Test
    @WithMockUser
    void createWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser
    void createWithDuplicatedNameReturns409() throws Exception {
        given(categoryService.create(any())).willThrow(new BusinessConflictException("Category with name 'x' already exists"));

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser
    void findAllReturns200() throws Exception {
        given(categoryService.findAll()).willReturn(List.of(
                new CategoryResponse(2L, "Eletrônicos", null), new CategoryResponse(1L, "Livros", null)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Eletrônicos"))
                .andExpect(jsonPath("$[1].name").value("Livros"));
    }

    @Test
    @WithMockUser
    void findByIdReturns200() throws Exception {
        given(categoryService.findById(1L)).willReturn(new CategoryResponse(1L, "Livros", "d"));

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Livros"));
    }

    @Test
    @WithMockUser
    void findByIdNotFoundReturns404() throws Exception {
        given(categoryService.findById(999L)).willThrow(new ResourceNotFoundException("Category", 999L));

        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.path").value("/api/categories/999"))
                .andExpect(jsonPath("$.message").value("Category with id 999 not found"));
    }

    @Test
    @WithMockUser
    void updateReturns200() throws Exception {
        given(categoryService.update(eq(1L), any())).willReturn(new CategoryResponse(1L, "Livros", "new"));

        mockMvc.perform(put("/api/categories/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Livros\",\"description\":\"new\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("new"));
    }

    @Test
    @WithMockUser
    void updateNotFoundReturns404() throws Exception {
        given(categoryService.update(eq(999L), any())).willThrow(new ResourceNotFoundException("Category", 999L));

        mockMvc.perform(put("/api/categories/999").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1")).andExpect(status().isNoContent());
        verify(categoryService).delete(1L);
    }

    @Test
    @WithMockUser
    void deleteWithProductsReturns409() throws Exception {
        willThrow(new BusinessConflictException("has products")).given(categoryService).delete(1L);

        mockMvc.perform(delete("/api/categories/1")).andExpect(status().isConflict());
    }

    @Test
    void requestWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/categories"));
        verifyNoInteractions(categoryService);
    }
}
