package io.dev.openspec.springbootopenspecapp;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.user.username=it-user",
        "app.security.user.password=it-password"
})
@AutoConfigureMockMvc
class CatalogApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String bearer;

    @BeforeEach
    void login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it-user\",\"password\":\"it-password\"}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();
        bearer = "Bearer " + JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    @Test
    void fullCatalogFlow() throws Exception {
        MvcResult categoryResult = mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IT Eletrônicos\",\"description\":\"Aparelhos\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andReturn();
        Integer categoryId = JsonPath.read(categoryResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/categories").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"it eletrônicos\"}"))
                .andExpect(status().isConflict());

        MvcResult productResult = mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Notebook\",\"price\":4500.00,\"stockQuantity\":10,\"categoryId\":"
                                + categoryId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category.id").value(categoryId))
                .andExpect(jsonPath("$.category.name").value("IT Eletrônicos"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andReturn();
        Integer productId = JsonPath.read(productResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/products").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"price\":1,\"stockQuantity\":1,\"categoryId\":999999}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products").param("categoryId", categoryId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(productId));

        mockMvc.perform(delete("/api/categories/" + categoryId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/products/" + productId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/categories/" + categoryId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/categories/" + categoryId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.instance").value("/api/products"))
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    @Test
    void notFoundIsProblemJsonEvenWhenClientAcceptsOnlyApplicationJson() throws Exception {
        mockMvc.perform(get("/api/products/999999").header(HttpHeaders.AUTHORIZATION, bearer)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:problem-type:resource-not-found"))
                .andExpect(jsonPath("$.detail").value("Product with id 999999 not found"))
                .andExpect(jsonPath("$.instance").value("/api/products/999999"));
    }

    @Test
    void unsupportedMethodReturns405Problem() throws Exception {
        mockMvc.perform(patch("/api/categories/1").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Method Not Allowed"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.instance").value("/api/categories/1"));
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String tampered = bearer.substring(0, bearer.length() - 4) + (bearer.endsWith("AAAA") ? "BBBB" : "AAAA");

        mockMvc.perform(get("/api/categories").header(HttpHeaders.AUTHORIZATION, tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"it-user\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("it-user");
    }
}
