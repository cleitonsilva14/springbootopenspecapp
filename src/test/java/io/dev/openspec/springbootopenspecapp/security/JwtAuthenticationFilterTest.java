package io.dev.openspec.springbootopenspecapp.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final JwtService jwtService = new JwtService(new JwtProperties(JwtServiceTest.SECRET, Duration.ofHours(1)));
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesRequest() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader("Authorization", "Bearer " + jwtService.generateToken("admin"));
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo("admin");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void missingHeaderLeavesContextEmpty() throws Exception {
        var chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/products"), new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void invalidTokenLeavesContextEmpty() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader("Authorization", "Bearer invalid.token.value");
        var chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void nonBearerSchemeIsIgnored() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/products");
        request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
