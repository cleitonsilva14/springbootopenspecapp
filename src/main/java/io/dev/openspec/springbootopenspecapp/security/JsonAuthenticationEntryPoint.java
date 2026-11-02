package io.dev.openspec.springbootopenspecapp.security;

import io.dev.openspec.springbootopenspecapp.exception.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String AUTHENTICATION_REQUIRED = "Authentication required: provide a valid Bearer token";

    private final ProblemDetailWriter problemDetailWriter;

    public JsonAuthenticationEntryPoint(ProblemDetailWriter problemDetailWriter) {
        this.problemDetailWriter = problemDetailWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        problemDetailWriter.write(request, response, ProblemType.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
    }
}
