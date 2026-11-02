package io.dev.openspec.springbootopenspecapp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, Duration expiration) {

    public JwtProperties {
        if (expiration == null) {
            expiration = Duration.ofHours(1);
        }
    }
}
