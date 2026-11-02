package io.dev.openspec.springbootopenspecapp.security;

import io.dev.openspec.springbootopenspecapp.exception.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    static final String ACCESS_DENIED = "Access denied";

    private final ProblemDetailWriter problemDetailWriter;

    public JsonAccessDeniedHandler(ProblemDetailWriter problemDetailWriter) {
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        problemDetailWriter.write(request, response, ProblemType.FORBIDDEN, ACCESS_DENIED);
    }
}
