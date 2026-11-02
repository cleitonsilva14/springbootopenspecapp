package io.dev.openspec.springbootopenspecapp.security;

import io.dev.openspec.springbootopenspecapp.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Importa a configuração real de segurança JWT em testes de slice {@code @WebMvcTest}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, JwtService.class, JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class, GlobalExceptionHandler.class})
public @interface WebSecurityTestImports {
}
