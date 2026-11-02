package io.dev.openspec.springbootopenspecapp.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void validationErrorReturns400WithAllFieldErrors() throws Exception {
        mockMvc.perform(post("/dummy").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/dummy"))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("name", "price")));
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/dummy").contentType(MediaType.APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void typeMismatchReturns400() throws Exception {
        mockMvc.perform(get("/dummy/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void resourceNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/dummy/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category with id 1 not found"))
                .andExpect(jsonPath("$.path").value("/dummy/1"));
    }

    @Test
    void entityNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/dummy/2")).andExpect(status().isNotFound());
    }

    @Test
    void businessConflictReturns409() throws Exception {
        mockMvc.perform(get("/dummy/3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("duplicated"));
    }

    @Test
    void dataIntegrityViolationReturns409() throws Exception {
        mockMvc.perform(get("/dummy/4")).andExpect(status().isConflict());
    }

    @Test
    void authenticationFailureReturns401WithGenericMessage() throws Exception {
        mockMvc.perform(get("/dummy/5"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.AUTH_MESSAGE));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/dummy/6"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.GENERIC_MESSAGE))
                .andExpect(content().string(not(containsString("SQL secret detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    record DummyRequest(@NotBlank String name, @Positive BigDecimal price) {
    }

    @RestController
    static class DummyController {

        @PostMapping("/dummy")
        void create(@Valid @RequestBody DummyRequest request) {
        }

        @GetMapping("/dummy/{id}")
        void get(@PathVariable Long id) {
            switch (id.intValue()) {
                case 1 -> throw new ResourceNotFoundException("Category", 1);
                case 2 -> throw new EntityNotFoundException("missing");
                case 3 -> throw new BusinessConflictException("duplicated");
                case 4 -> throw new DataIntegrityViolationException("constraint");
                case 5 -> throw new BadCredentialsException("bad user admin");
                default -> throw new IllegalStateException("SQL secret detail");
            }
        }
    }
}
