package io.dev.openspec.springbootopenspecapp.security;

import io.dev.openspec.springbootopenspecapp.exception.ProblemDetails;
import io.dev.openspec.springbootopenspecapp.exception.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escreve respostas RFC 9457 (application/problem+json) a partir da cadeia de filtros de segurança,
 * onde o {@code GlobalExceptionHandler} não atua.
 */
@Component
public class ProblemDetailWriter {

    private final ObjectMapper objectMapper;

    public ProblemDetailWriter(ObjectMapper objectMapper) {
        // Garante que as propriedades de extensão do ProblemDetail sejam serializadas no nível raiz do JSON,
        // independentemente da configuração do mapper recebido.
        this.objectMapper = objectMapper.rebuild()
                .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
                .build();
    }

    public void write(HttpServletRequest request, HttpServletResponse response, ProblemType type, String detail)
            throws IOException {
        ProblemDetail problem = ProblemDetails.of(type, detail, request.getRequestURI());
        response.setStatus(type.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
