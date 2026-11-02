package io.dev.openspec.springbootopenspecapp.exception;

import org.springframework.http.HttpStatus;

import java.net.URI;

/**
 * Catálogo dos tipos de problema (RFC 9457) emitidos pela aplicação.
 * Cada tipo tem um {@code type} estável ({@code urn:problem-type:<slug>}), um {@code title} fixo e o status HTTP.
 */
public enum ProblemType {

    VALIDATION_ERROR("validation-error", "Validation failed", HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST("malformed-request", "Malformed request body", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER("invalid-parameter", "Invalid parameter", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("unauthorized", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("forbidden", "Forbidden", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("resource-not-found", "Resource not found", HttpStatus.NOT_FOUND),
    BUSINESS_CONFLICT("business-conflict", "Business rule conflict", HttpStatus.CONFLICT),
    DATA_INTEGRITY_VIOLATION("data-integrity-violation", "Data integrity violation", HttpStatus.CONFLICT),
    INTERNAL_ERROR("internal-error", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    public static final String URN_PREFIX = "urn:problem-type:";

    private final String slug;
    private final String title;
    private final HttpStatus status;

    ProblemType(String slug, String title, HttpStatus status) {
        this.slug = slug;
        this.title = title;
        this.status = status;
    }

    public String slug() {
        return slug;
    }

    public String title() {
        return title;
    }

    public HttpStatus status() {
        return status;
    }

    public URI uri() {
        return URI.create(URN_PREFIX + slug);
    }
}
