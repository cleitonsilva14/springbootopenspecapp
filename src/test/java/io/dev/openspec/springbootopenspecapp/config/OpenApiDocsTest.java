package io.dev.openspec.springbootopenspecapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    private static final String PROBLEM_DETAIL_REF = "#/components/schemas/ProblemDetail";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsArePublicAndDescribeEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Product Catalog API"))
                .andExpect(jsonPath("$.info.version").exists())
                .andExpect(jsonPath("$.paths", hasKey("/api/auth/login")))
                .andExpect(jsonPath("$.paths", hasKey("/api/categories")))
                .andExpect(jsonPath("$.paths", hasKey("/api/products")))
                .andExpect(jsonPath("$.paths", hasKey("/api/products/{id}")))
                .andExpect(jsonPath("$.paths", hasKey("/api/categories/{id}")));
    }

    @Test
    void bearerSecuritySchemeIsDeclaredAndApplied() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/products'].get.security[0]", hasKey("bearerAuth")))
                .andExpect(jsonPath("$.paths['/api/categories'].post.security[0]", hasKey("bearerAuth")))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").doesNotExist())
                .andExpect(jsonPath("$.security").doesNotExist());
    }

    @Test
    void createProductDocumentsErrorResponses() throws Exception {
        String responses = "$.paths['/api/products'].post.responses";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(responses, hasKey("201")))
                .andExpect(jsonPath(responses, hasKey("400")))
                .andExpect(jsonPath(responses, hasKey("401")))
                .andExpect(jsonPath(responses, hasKey("404")))
                .andExpect(jsonPath(responses + "['400'].content['application/problem+json'].schema['$ref']")
                        .value(PROBLEM_DETAIL_REF))
                .andExpect(jsonPath(responses + "['401'].content['application/problem+json'].schema['$ref']")
                        .value(PROBLEM_DETAIL_REF))
                .andExpect(jsonPath(responses + "['404'].content['application/problem+json'].schema['$ref']")
                        .value(PROBLEM_DETAIL_REF))
                .andExpect(jsonPath("$.components.schemas", not(hasKey("ErrorResponse"))));
    }

    @Test
    void problemDetailSchemaIsPublishedWithRfc9457Members() throws Exception {
        String properties = "$.components.schemas.ProblemDetail.properties";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(properties, hasKey("type")))
                .andExpect(jsonPath(properties, hasKey("title")))
                .andExpect(jsonPath(properties, hasKey("status")))
                .andExpect(jsonPath(properties, hasKey("detail")))
                .andExpect(jsonPath(properties, hasKey("instance")))
                .andExpect(jsonPath(properties, hasKey("fieldErrors")))
                .andExpect(jsonPath(properties + ".type.example").value("urn:problem-type:resource-not-found"));
    }

    @Test
    void swaggerUiIsPublic() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
