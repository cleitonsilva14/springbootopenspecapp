package io.dev.openspec.springbootopenspecapp.security;

import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    static final String SECRET = Encoders.BASE64.encode(
            "test-secret-with-at-least-32-bytes-long!".getBytes(StandardCharsets.UTF_8));
    static final String OTHER_SECRET = Encoders.BASE64.encode(
            "another-secret-with-32-bytes-or-more!!!!".getBytes(StandardCharsets.UTF_8));

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1)));

    @Test
    void generatesValidTokenWithSubject() {
        String token = jwtService.generateToken("admin");

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
        assertThat(jwtService.getExpiration()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtService expiring = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-5)));
        String token = expiring.generateToken("admin");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherSecretIsInvalid() {
        JwtService other = new JwtService(new JwtProperties(OTHER_SECRET, Duration.ofHours(1)));
        String token = other.generateToken("admin");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void tamperedTokenIsInvalid() {
        String token = jwtService.generateToken("admin");
        String tampered = token.substring(0, token.length() - 3) + (token.endsWith("aaa") ? "bbb" : "aaa");

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void malformedTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    void shortSecretFailsAtStartup() {
        String shortSecret = Encoders.BASE64.encode("short".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> new JwtService(new JwtProperties(shortSecret, Duration.ofHours(1))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    void missingSecretFailsAtStartup() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(" ", Duration.ofHours(1))))
                .isInstanceOf(IllegalStateException.class);
    }
}
