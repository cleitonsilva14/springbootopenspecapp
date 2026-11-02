package io.dev.openspec.springbootopenspecapp.common.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;

import java.net.URI;
import java.util.List;

/**
 * Schema de documentação (OpenAPI) do corpo de erro RFC 9457. Nunca é instanciado: em tempo de execução
 * a API devolve {@link org.springframework.http.ProblemDetail}, cuja extensão {@code fieldErrors} é achatada
 * no nível raiz do JSON.
 */
@Schema(name = "ProblemDetail",
        description = "Detalhes de erro no formato RFC 9457 (Problem Details for HTTP APIs), "
                + "servido como application/problem+json")
public record ProblemDetailSchema(
        @Schema(description = "URI que identifica o tipo de problema",
                example = "urn:problem-type:resource-not-found", requiredMode = Schema.RequiredMode.REQUIRED)
        URI type,

        @Schema(description = "Resumo legível do tipo de problema",
                example = "Resource not found", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "Código de status HTTP desta ocorrência",
                example = "404", requiredMode = Schema.RequiredMode.REQUIRED)
        int status,

        @Schema(description = "Explicação legível específica desta ocorrência",
                example = "Category with id 999 not found", requiredMode = Schema.RequiredMode.REQUIRED)
        String detail,

        @Schema(description = "Caminho da requisição que originou o erro",
                example = "/api/categories/999", requiredMode = Schema.RequiredMode.REQUIRED)
        URI instance,

        @ArraySchema(arraySchema = @Schema(description = "Campos inválidos (presente apenas em erros de validação)",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED))
        List<FieldErrorItem> fieldErrors
) {

    /** Media type das respostas de erro, para uso em {@code @Content(mediaType = ...)}. */
    public static final String MEDIA_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
}
