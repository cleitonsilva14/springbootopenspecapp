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

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void entryPointWritesStandard401Body() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/products");
        var response = new MockHttpServletResponse();

        new JsonAuthenticationEntryPoint(objectMapper)
                .commence(request, response, new InsufficientAuthenticationException("no token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asString()).isEqualTo("Unauthorized");
        assertThat(body.get("path").asString()).isEqualTo("/api/products");
        assertThat(body.hasNonNull("timestamp")).isTrue();
        assertThat(body.hasNonNull("message")).isTrue();
    }

    @Test
    void accessDeniedHandlerWritesStandard403Body() throws Exception {
        var request = new MockHttpServletRequest("DELETE", "/api/categories/1");
        var response = new MockHttpServletResponse();

        new JsonAccessDeniedHandler(objectMapper).handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("path").asString()).isEqualTo("/api/categories/1");
    }
}
