package io.dev.openspec.springbootopenspecapp.exception;

import io.dev.openspec.springbootopenspecapp.common.dto.FieldErrorItem;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.List;

/**
 * Converte exceções em respostas RFC 9457 (Problem Details) com {@code Content-Type: application/problem+json}.
 * Exceções padrão do Spring MVC sem tipo próprio herdam o tratamento de {@link ResponseEntityExceptionHandler}
 * ({@code type} "about:blank"), passando pelo mesmo ponto de saída ({@link #createResponseEntity}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    static final String MALFORMED_MESSAGE = "Request body is missing or is not valid JSON";
    static final String AUTH_MESSAGE = "Invalid username or password";
    static final String ACCESS_DENIED_MESSAGE = "Access denied";
    static final String DATA_INTEGRITY_MESSAGE = "Operation violates a data integrity constraint";
    static final String GENERIC_MESSAGE = "An unexpected error occurred";

    // ---- Sobrescritas de ResponseEntityExceptionHandler (sem @ExceptionHandler duplicado) ----

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<FieldErrorItem> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return validationProblem(fieldErrors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldErrorItem> fieldErrors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorItem(
                                result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return validationProblem(fieldErrors, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        return problem(ProblemType.MALFORMED_REQUEST, MALFORMED_MESSAGE, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String parameter = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        String detail = "Invalid value '" + ex.getValue() + "' for parameter '" + parameter + "'";
        return problem(ProblemType.INVALID_PARAMETER, detail, headers, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException ex, HttpHeaders headers,
                                                                    HttpStatusCode status, WebRequest request) {
        return problem(ProblemType.RESOURCE_NOT_FOUND, "No resource found for " + path(request), headers, request);
    }

    // ---- Exceções da aplicação e de segurança ----

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return problem(ProblemType.UNAUTHORIZED, AUTH_MESSAGE, new HttpHeaders(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return problem(ProblemType.FORBIDDEN, ACCESS_DENIED_MESSAGE, new HttpHeaders(), request);
    }

    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<Object> handleNotFound(RuntimeException ex, WebRequest request) {
        return problem(ProblemType.RESOURCE_NOT_FOUND, ex.getMessage(), new HttpHeaders(), request);
    }

    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<Object> handleConflict(BusinessConflictException ex, WebRequest request) {
        return problem(ProblemType.BUSINESS_CONFLICT, ex.getMessage(), new HttpHeaders(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Data integrity violation on {}: {}", path(request), ex.getMostSpecificCause().getMessage());
        return problem(ProblemType.DATA_INTEGRITY_VIOLATION, DATA_INTEGRITY_MESSAGE, new HttpHeaders(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex, WebRequest request) {
        HttpServletRequest servletRequest = servletRequest(request);
        log.error("Unexpected error on {} {}",
                servletRequest != null ? servletRequest.getMethod() : "?", path(request), ex);
        return problem(ProblemType.INTERNAL_ERROR, GENERIC_MESSAGE, new HttpHeaders(), request);
    }

    // ---- Ponto único de saída ----

    /**
     * Toda resposta de erro (da aplicação ou padrão do framework) passa por aqui: garante {@code type}
     * ({@code about:blank} quando o framework não define um), preenche {@code instance} com o caminho da
     * requisição quando ausente e força {@code Content-Type: application/problem+json}.
     */
    @Override
    protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            if (problem.getType() == null) {
                problem.setType(ABOUT_BLANK);
            }
            String path = path(request);
            if (problem.getInstance() == null && path != null) {
                problem.setInstance(URI.create(path));
            }
        }
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(headers);
        responseHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return new ResponseEntity<>(body, responseHeaders, statusCode);
    }

    private ResponseEntity<Object> validationProblem(List<FieldErrorItem> fieldErrors, HttpHeaders headers,
                                                     WebRequest request) {
        ProblemDetail problem = ProblemDetails.of(ProblemType.VALIDATION_ERROR,
                "Request has " + fieldErrors.size() + " invalid field(s)", path(request));
        problem.setProperty(ProblemDetails.FIELD_ERRORS, fieldErrors);
        return createResponseEntity(problem, headers, ProblemType.VALIDATION_ERROR.status(), request);
    }

    private ResponseEntity<Object> problem(ProblemType type, String detail, HttpHeaders headers, WebRequest request) {
        return createResponseEntity(ProblemDetails.of(type, detail, path(request)), headers, type.status(), request);
    }

    private FieldErrorItem toFieldError(FieldError error) {
        return new FieldErrorItem(error.getField(), error.getDefaultMessage());
    }

    private static @Nullable String path(WebRequest request) {
        HttpServletRequest servletRequest = servletRequest(request);
        return servletRequest != null ? servletRequest.getRequestURI() : null;
    }

    private static @Nullable HttpServletRequest servletRequest(WebRequest request) {
        return request instanceof NativeWebRequest nativeRequest
                ? nativeRequest.getNativeRequest(HttpServletRequest.class)
                : null;
    }
}
