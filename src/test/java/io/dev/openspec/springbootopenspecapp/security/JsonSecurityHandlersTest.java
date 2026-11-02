package io.dev.openspec.springbootopenspecapp.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSecurityHandlersTest {

    /** Mapper sem nenhuma configuração extra: o writer deve aplicar o mixin do ProblemDetail por conta própria. */
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final ProblemDetailWriter writer = new ProblemDetailWriter(objectMapper);

    @Test
    void entryPointWritesRfc9457Unauthorized() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/products");
        var response = new MockHttpServletResponse();

        new JsonAuthenticationEntryPoint(writer)
                .commence(request, response, new InsufficientAuthenticationException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("type").asString()).isEqualTo("urn:problem-type:unauthorized");
        assertThat(body.get("title").asString()).isEqualTo("Unauthorized");
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("detail").asString()).isEqualTo(JsonAuthenticationEntryPoint.AUTHENTICATION_REQUIRED);
        assertThat(body.get("instance").asString()).isEqualTo("/api/products");
        assertOldFormatAbsent(body);
    }

    @Test
    void accessDeniedHandlerWritesRfc9457Forbidden() throws Exception {
        var request = new MockHttpServletRequest("DELETE", "/api/categories/1");
        var response = new MockHttpServletResponse();

        new JsonAccessDeniedHandler(writer).handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/problem+json");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("type").asString()).isEqualTo("urn:problem-type:forbidden");
        assertThat(body.get("title").asString()).isEqualTo("Forbidden");
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("detail").asString()).isEqualTo(JsonAccessDeniedHandler.ACCESS_DENIED);
        assertThat(body.get("instance").asString()).isEqualTo("/api/categories/1");
        assertOldFormatAbsent(body);
    }

    private static void assertOldFormatAbsent(JsonNode body) {
        assertThat(body.has("timestamp")).isFalse();
        assertThat(body.has("error")).isFalse();
        assertThat(body.has("message")).isFalse();
        assertThat(body.has("path")).isFalse();
        assertThat(body.has("properties")).isFalse();
    }
}
