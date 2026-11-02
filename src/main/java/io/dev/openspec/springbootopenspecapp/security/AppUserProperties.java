package io.dev.openspec.springbootopenspecapp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.security.user")
public record AppUserProperties(String username, String password) {
}
