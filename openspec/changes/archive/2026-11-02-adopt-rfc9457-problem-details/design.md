# Design

## Context

Motivação: ver `proposal.md`. Requisitos: `specs/api-error-handling/spec.md` e `specs/api-documentation/spec.md`.

Estado atual (`<base>` = `io.dev.openspec.springbootopenspecapp`):

- `exception.GlobalExceptionHandler` (`@RestControllerAdvice`) tem um `@ExceptionHandler` por exceção e monta `ResponseEntity<ErrorResponse>` via `build(...)`. Não estende `ResponseEntityExceptionHandler`. Por isso `HttpRequestMethodNotSupportedException`, `HttpMediaTypeNotSupportedException` etc. caem em `handleGeneric` e viram 500.
- `security.SecurityErrorWriter` (estático) serializa `ErrorResponse` com o `tools.jackson.databind.ObjectMapper` injetado em `JsonAuthenticationEntryPoint` (401) e `JsonAccessDeniedHandler` (403), com `Content-Type: application/json`.
- Controllers e `AuthController` referenciam `ErrorResponse.class` em `@ApiResponse(content = @Content(schema = ...))`.
- Stack: Spring Boot 4.1.2, Spring Framework 7.0.10, Jackson 3.1.7 (`tools.jackson`), Springdoc 3.2.0.
- O `JacksonJsonHttpMessageConverter` do Spring 7 registra `ProblemDetailJacksonMixin` (que achata o mapa `properties` com `@JsonAnyGetter`) no mapper que ele constrói. Não há garantia de que o `ObjectMapper` injetado pelo Boot tenha esse mixin, e sem ele `fieldErrors` sairia aninhado em `"properties": {...}`.
- Testes afetados: `GlobalExceptionHandlerTest` (standalone MockMvc), `JsonSecurityHandlersTest`, `CategoryControllerTest`, `ProductControllerTest`, `AuthControllerTest`, `CatalogApiIntegrationTest`, `OpenApiDocsTest` (verifica `#/components/schemas/ErrorResponse`).

## Goals / Non-Goals

**Goals:**
- Uma única fonte para os tipos de problema (type/title/status), usada pelo advice e pelos handlers de segurança.
- `fieldErrors` serializado no nível raiz do JSON, e não dentro de `properties`.
- `Content-Type: application/problem+json` em toda resposta de erro, inclusive com `Accept: application/json`.

**Non-Goals:**
- Internacionalização de `title`/`detail` via `MessageSource`.
- Publicar páginas de documentação para cada `type` (as URIs são URNs, não URLs resolvíveis).
- Adicionar membros de extensão além de `fieldErrors` (por exemplo, `timestamp` ou `traceId`).
- Mudar regras de negócio ou status HTTP dos erros já mapeados.

## Decisions

### D1. `ProblemDetail` nativo do Spring em vez de classe própria
Em tempo de execução, usar `org.springframework.http.ProblemDetail`, com `setProperty("fieldErrors", List<FieldErrorItem>)` para a extensão. O Spring já o serializa conforme a RFC e o `ResponseEntityExceptionHandler` já o produz.
*Alternativa:* um record próprio `ApiProblem`. Foi descartado porque duplicaria o que o framework oferece e não se integraria ao `ResponseEntityExceptionHandler`.

### D2. Catálogo de tipos: enum `ProblemType` em `exception`
`ProblemType` define slug, `title` e `HttpStatus`. O `type` é `URI.create("urn:problem-type:" + slug)`. Uma classe utilitária `ProblemDetails` em `exception` monta a instância:
`ProblemDetails.of(ProblemType type, String detail, String instance)`, que chama `ProblemDetail.forStatusAndDetail(...)` e preenche `type`, `title` e `instance`.

| ProblemType | status | title | detail |
|---|---|---|---|
| `VALIDATION_ERROR` | 400 | Validation failed | `Request has N invalid field(s)` + `fieldErrors` |
| `MALFORMED_REQUEST` | 400 | Malformed request body | `Request body is missing or is not valid JSON` |
| `INVALID_PARAMETER` | 400 | Invalid parameter | `Invalid value 'abc' for parameter 'id'` |
| `UNAUTHORIZED` | 401 | Unauthorized | `Invalid username or password` (login) / `Authentication required: provide a valid Bearer token` (filtro) |
| `FORBIDDEN` | 403 | Forbidden | `Access denied` |
| `RESOURCE_NOT_FOUND` | 404 | Resource not found | mensagem da exceção (`Category with id 42 not found`) / `No resource found for <path>` |
| `BUSINESS_CONFLICT` | 409 | Business rule conflict | mensagem da `BusinessConflictException` |
| `DATA_INTEGRITY_VIOLATION` | 409 | Data integrity violation | `Operation violates a data integrity constraint` |
| `INTERNAL_ERROR` | 500 | Internal server error | `An unexpected error occurred` |

*URN em vez de URL:* o projeto não tem site de documentação de erros, e a RFC 9457 aceita URIs não resolvíveis. *Alternativa:* `about:blank` para tudo, descartada porque o cliente não conseguiria distinguir os casos de 400 nem os de 409 entre si.

### D3. `GlobalExceptionHandler extends ResponseEntityExceptionHandler`
- **Sobrescritas** (sem `@ExceptionHandler` duplicado, que geraria mapeamento ambíguo na inicialização): `handleMethodArgumentNotValid` → `VALIDATION_ERROR`; `handleHandlerMethodValidationException` → `VALIDATION_ERROR` (mantendo a extração atual de `fieldErrors`); `handleHttpMessageNotReadable` → `MALFORMED_REQUEST`; `handleTypeMismatch` → `INVALID_PARAMETER` (o `ResponseEntityExceptionHandler` já trata `TypeMismatchException`, superclasse de `MethodArgumentTypeMismatchException`); `handleNoResourceFoundException` → `RESOURCE_NOT_FOUND`.
- **`@ExceptionHandler` próprios** para o que o `ResponseEntityExceptionHandler` não cobre: `AuthenticationException`, `AccessDeniedException`, `ResourceNotFoundException`/`EntityNotFoundException`, `BusinessConflictException`, `DataIntegrityViolationException` (mantém o `log.warn`) e `Exception` (mantém o `log.error`).
- **Demais exceções do framework** (405, 406, 415, 400 de parâmetro ausente etc.): ficam com o comportamento padrão do `ResponseEntityExceptionHandler` (`type` `about:blank`, `title` = frase do status).
- **Ponto único de saída:** todas as respostas passam por um método privado `respond(ProblemDetail, HttpHeaders)`. Esse método preenche `type` com `about:blank` quando estiver ausente (no Spring 7 o `ProblemDetail` deixa `type` nulo por padrão e o Jackson o omitiria), preenche `instance` com `request.getRequestURI()` quando estiver ausente e devolve `ResponseEntity.status(...).headers(...).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem)`. Ele também é usado na sobrescrita de `createResponseEntity(...)`, para que as respostas padrão do framework recebam o mesmo `Content-Type` e `instance`.
- **Motivo do `Content-Type` explícito:** quando ele já vem definido na resposta, o Spring o usa como media type selecionado. Assim a resposta sai como `application/problem+json` mesmo com `Accept: application/json`, sem `406`.
- Para obter o caminho da requisição nas sobrescritas, usar o `WebRequest` recebido: `((ServletWebRequest) request).getRequest().getRequestURI()`.

### D4. Handlers de segurança: bean `ProblemDetailWriter`
Substituir o utilitário estático `SecurityErrorWriter` por um `@Component ProblemDetailWriter` em `security`. Ele recebe o `ObjectMapper` do Boot e, no construtor, cria um mapper derivado: `objectMapper.rebuild().addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class).build()`. O método `write(request, response, ProblemType, detail)` grava o status, `Content-Type: application/problem+json`, UTF-8 e o corpo gerado por `ProblemDetails.of(...)`. `JsonAuthenticationEntryPoint` e `JsonAccessDeniedHandler` passam a injetar o writer. `WebSecurityTestImports` inclui `ProblemDetailWriter.class`.
*Alternativa:* delegar ao `HandlerExceptionResolver` a partir do entry point, para reaproveitar o advice. Foi descartada porque mistura a mensagem do login com a do filtro e acopla a segurança ao MVC.

### D5. Documentação OpenAPI: record `ProblemDetailSchema` só para documentação
Criar `common.dto.ProblemDetailSchema` com `@Schema(name = "ProblemDetail", description = "RFC 9457 Problem Details")` e os campos `type` (URI, example `urn:problem-type:resource-not-found`), `title`, `status`, `detail`, `instance` e `fieldErrors` (`List<FieldErrorItem>`, não obrigatório). Ele nunca é instanciado: em tempo de execução vale o `ProblemDetail` do Spring.
- Trocar, em `CategoryController`, `ProductController` e `AuthController`, `@Content(schema = @Schema(implementation = ErrorResponse.class))` por `@Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetailSchema.class))`.
- Para não repetir o media type em ~25 anotações, criar a constante `ProblemDetailSchema.MEDIA_TYPE = MediaType.APPLICATION_PROBLEM_JSON_VALUE`.

*Alternativa:* `@Schema(implementation = ProblemDetail.class)`. Foi descartada porque o Springdoc exporia o mapa `properties` em vez de `fieldErrors`.

### D6. Remoção de `ErrorResponse`
Remover `common.dto.ErrorResponse` depois que não restar nenhuma referência. `FieldErrorItem` permanece.

### Exemplos de payload esperados

404 (`GET /api/categories/999`):
```json
{
  "type": "urn:problem-type:resource-not-found",
  "title": "Resource not found",
  "status": 404,
  "detail": "Category with id 999 not found",
  "instance": "/api/categories/999"
}
```

400 de validação (`POST /api/products` com `name` vazio e `price` negativo):
```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request has 2 invalid field(s)",
  "instance": "/api/products",
  "fieldErrors": [
    { "field": "name", "message": "must not be blank" },
    { "field": "price", "message": "must be greater than 0" }
  ]
}
```

401 (`GET /api/products` sem token):
```json
{
  "type": "urn:problem-type:unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authentication required: provide a valid Bearer token",
  "instance": "/api/products"
}
```

409 (categoria duplicada):
```json
{
  "type": "urn:problem-type:business-conflict",
  "title": "Business rule conflict",
  "status": 409,
  "detail": "Category with name 'Books' already exists",
  "instance": "/api/categories"
}
```

500:
```json
{
  "type": "urn:problem-type:internal-error",
  "title": "Internal server error",
  "status": 500,
  "detail": "An unexpected error occurred",
  "instance": "/api/products"
}
```

405 (padrão do framework):
```json
{
  "type": "about:blank",
  "title": "Method Not Allowed",
  "status": 405,
  "detail": "Method 'PATCH' is not supported.",
  "instance": "/api/categories/1"
}
```

As mensagens exatas de `fieldErrors` vêm das anotações de validação dos DTOs. O `detail` de 405 vem do Spring.

## Risks / Trade-offs

- [`@ExceptionHandler` para uma exceção já tratada pelo `ResponseEntityExceptionHandler` (ex.: `MethodArgumentNotValidException`) → `IllegalStateException: Ambiguous @ExceptionHandler` na inicialização] → tratar essas exceções somente por sobrescrita (D3). O `SpringbootopenspecappApplicationTests` pega o problema na subida do contexto.
- [`fieldErrors` serializado dentro de `properties` no writer de segurança, ou se o converter do MVC não aplicar o mixin] → mixin explícito no `ProblemDetailWriter` (D4) e testes que verificam `$.fieldErrors` na raiz e `$.properties` inexistente, tanto via MVC quanto via writer.
- [Clientes existentes quebram ao procurar `message`/`path`] → mudança marcada como **BREAKING** na proposal. Os status HTTP não mudam.
- [O `GlobalExceptionHandlerTest` standalone não usa o mapper do Boot] → o `standaloneSetup` usa os converters padrão do Spring, que já registram o mixin. Testes `@WebMvcTest`/`@SpringBootTest` cobrem o mapper real.
- [`ResponseEntityExceptionHandler` registra `log.warn` para algumas exceções e muda o comportamento de log] → aceitável. Sem impacto no contrato.

## Migration Plan

Deploy único, sem migração de dados. Clientes devem passar a ler `detail`/`instance`/`title` e aceitar `application/problem+json`. O rollback é reverter o artefato. Não há versão de API paralela.
