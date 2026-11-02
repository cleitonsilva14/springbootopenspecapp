package io.dev.openspec.springbootopenspecapp.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    /** Verifica os membros obrigatórios da RFC 9457, o Content-Type e a ausência do formato antigo. */
    private static ResultActions expectProblem(ResultActions actions, ProblemType type, String instance) throws Exception {
        return expectProblem(actions, type.uri().toString(), type.title(), type.status().value(), instance);
    }

    private static ResultActions expectProblem(ResultActions actions, String type, String title, int status,
                                               String instance) throws Exception {
        return actions
                .andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value(instance))
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist())
                .andExpect(jsonPath("$.properties").doesNotExist());
    }

    @Test
    void validationErrorReturns400WithAllFieldErrorsAtRootLevel() throws Exception {
        expectProblem(mockMvc.perform(post("/dummy").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-1}")), ProblemType.VALIDATION_ERROR, "/dummy")
                .andExpect(jsonPath("$.detail").value("Request has 2 invalid field(s)"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("name", "price")))
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        expectProblem(mockMvc.perform(post("/dummy").contentType(MediaType.APPLICATION_JSON).content("{invalid")),
                ProblemType.MALFORMED_REQUEST, "/dummy")
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.MALFORMED_MESSAGE))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void typeMismatchReturns400NamingTheParameter() throws Exception {
        expectProblem(mockMvc.perform(get("/dummy/abc")), ProblemType.INVALID_PARAMETER, "/dummy/abc")
                .andExpect(jsonPath("$.detail").value("Invalid value 'abc' for parameter 'id'"));
    }

    @Test
    void authenticationFailureReturns401WithGenericDetail() throws Exception {
        expectProblem(mockMvc.perform(get("/dummy/5")), ProblemType.UNAUTHORIZED, "/dummy/5")
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.AUTH_MESSAGE))
                .andExpect(content().string(not(containsString("bad user admin"))));
    }

    @Test
    void accessDeniedReturns403() throws Exception {
        expectProblem(mockMvc.perform(get("/dummy/7")), ProblemType.FORBIDDEN, "/dummy/7")
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.ACCESS_DENIED_MESSAGE));
    }

    @Test
    void resourceNotFoundReturns404() throws Exception {
        expectProblem(mockMvc.perform(get("/dummy/1")), ProblemType.RESOURCE_NOT_FOUND, "/dummy/1")
                .andExpect(jsonPath("$.detail").value("Category with id 1 not found"));
    }

    @Test
    void entityNotFoundReturns404() throws Exception {
        expectProblem(mockMvc.perform(get("/dummy/2")), ProblemType.RESOURCE_NOT_FOUND, "/dummy/2")
                .andExpect(jsonPath("$.detail").value("missing"));
    }

    @Test
    void businessConflictReturns409() throws Exception {
        expectProblem(mockMvc.perform(get("/dummy/3")), ProblemType.BUSINESS_CONFLICT, "/dummy/3")
                .andExpect(jsonPath("$.detail").value("duplicated"));
    }

    @Test
    void dataIntegrityViolationReturns409WithoutSqlDetail() throws Exception {
        expectProblem(mockMvc.perform(get("/dummy/4")), ProblemType.DATA_INTEGRITY_VIOLATION, "/dummy/4")
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.DATA_INTEGRITY_MESSAGE))
                .andExpect(content().string(not(containsString("constraint violated in SQL"))));
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() throws Exception {
        expectProblem(mockMvc.perform(get("/dummy/6")), ProblemType.INTERNAL_ERROR, "/dummy/6")
                .andExpect(jsonPath("$.detail").value(GlobalExceptionHandler.GENERIC_MESSAGE))
                .andExpect(content().string(not(containsString("SQL secret detail"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at io.dev"))));
    }

    @Test
    void unsupportedMethodReturns405AsAboutBlankProblem() throws Exception {
        expectProblem(mockMvc.perform(patch("/dummy/1")), "about:blank", "Method Not Allowed", 405, "/dummy/1");
    }

    @Test
    void unsupportedMediaTypeReturns415AsAboutBlankProblem() throws Exception {
        expectProblem(mockMvc.perform(post("/dummy").contentType(MediaType.TEXT_PLAIN).content("x")),
                "about:blank", "Unsupported Media Type", 415, "/dummy");
    }

    @Test
    void problemJsonIsReturnedEvenWhenClientAcceptsOnlyApplicationJson() throws Exception {
        expectProblem(mockMvc.perform(get("/dummy/1").accept(MediaType.APPLICATION_JSON)),
                ProblemType.RESOURCE_NOT_FOUND, "/dummy/1");
    }

    record DummyRequest(@NotBlank String name, @Positive BigDecimal price) {
    }

    @RestController
    static class DummyController {

        @PostMapping(value = "/dummy", consumes = MediaType.APPLICATION_JSON_VALUE)
        void create(@Valid @RequestBody DummyRequest request) {
        }

        @GetMapping("/dummy/{id}")
        void get(@PathVariable Long id) {
            switch (id.intValue()) {
                case 1 -> throw new ResourceNotFoundException("Category", 1);
                case 2 -> throw new EntityNotFoundException("missing");
                case 3 -> throw new BusinessConflictException("duplicated");
                case 4 -> throw new DataIntegrityViolationException("constraint violated in SQL");
                case 5 -> throw new BadCredentialsException("bad user admin");
                case 7 -> throw new AccessDeniedException("no role");
                default -> throw new IllegalStateException("SQL secret detail");
            }
        }
    }
}
