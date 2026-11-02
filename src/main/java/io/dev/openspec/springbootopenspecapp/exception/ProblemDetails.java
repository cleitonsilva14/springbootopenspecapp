package io.dev.openspec.springbootopenspecapp.exception;

import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * Fábrica de {@link ProblemDetail} (RFC 9457) a partir de um {@link ProblemType}.
 */
public final class ProblemDetails {

    public static final String FIELD_ERRORS = "fieldErrors";

    private ProblemDetails() {
    }

    public static ProblemDetail of(ProblemType type, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(type.status(), detail);
        problem.setType(type.uri());
        problem.setTitle(type.title());
        if (instance != null) {
            problem.setInstance(URI.create(instance));
        }
        return problem;
    }
}
