package io.dev.openspec.springbootopenspecapp.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsTest {

    @ParameterizedTest
    @EnumSource(ProblemType.class)
    void everyTypeHasStableUrnTitleAndStatus(ProblemType type) {
        assertThat(type.uri()).isEqualTo(URI.create("urn:problem-type:" + type.slug()));
        assertThat(type.slug()).matches("[a-z]+(-[a-z]+)*");
        assertThat(type.title()).isNotBlank();
        assertThat(type.status().isError()).isTrue();
    }

    @Test
    void catalogMatchesDesignTable() {
        assertThat(ProblemType.VALIDATION_ERROR.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ProblemType.MALFORMED_REQUEST.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ProblemType.INVALID_PARAMETER.status()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ProblemType.UNAUTHORIZED.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ProblemType.FORBIDDEN.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(ProblemType.RESOURCE_NOT_FOUND.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ProblemType.BUSINESS_CONFLICT.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ProblemType.DATA_INTEGRITY_VIOLATION.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ProblemType.INTERNAL_ERROR.status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(ProblemType.RESOURCE_NOT_FOUND.uri()).hasToString("urn:problem-type:resource-not-found");
    }

    @Test
    void ofFillsAllRfc9457Members() {
        ProblemDetail problem = ProblemDetails.of(ProblemType.RESOURCE_NOT_FOUND,
                "Category with id 999 not found", "/api/categories/999");

        assertThat(problem.getType()).hasToString("urn:problem-type:resource-not-found");
        assertThat(problem.getTitle()).isEqualTo("Resource not found");
        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getDetail()).isEqualTo("Category with id 999 not found");
        assertThat(problem.getInstance()).hasToString("/api/categories/999");
        assertThat(problem.getProperties()).isNullOrEmpty();
    }

    @Test
    void ofWithoutInstanceLeavesItUnset() {
        ProblemDetail problem = ProblemDetails.of(ProblemType.INTERNAL_ERROR, "An unexpected error occurred", null);

        assertThat(problem.getInstance()).isNull();
    }
}
