# Proposal

## Why

Hoje a API devolve erros num formato proprietário (`ErrorResponse`: `timestamp`, `status`, `error`, `message`, `path`, `fieldErrors`) com `Content-Type: application/json`. Clientes e ferramentas precisam de tratamento específico para esse formato. A RFC 9457 (Problem Details for HTTP APIs) define um formato padrão (`application/problem+json`) que o Spring Framework 7 já suporta nativamente via `ProblemDetail`. Adotá-lo torna os erros interoperáveis e autoexplicativos.

Há ainda uma lacuna: exceções do próprio Spring MVC que hoje não têm handler (método HTTP não suportado, media type não suportado/aceitável) caem no handler genérico e viram `500`, quando deveriam ser `405`/`415`/`406`.

## What Changes

- **BREAKING**: todas as respostas de erro (4xx/5xx), tanto do `GlobalExceptionHandler` quanto dos handlers de segurança (401/403), passam a usar o formato RFC 9457 com os membros `type`, `title`, `status`, `detail` e `instance`. Os campos `timestamp`, `error`, `message` e `path` deixam de existir.
- **BREAKING**: o `Content-Type` das respostas de erro passa a ser `application/problem+json`.
- Erros de validação (`@Valid` no corpo e validação de parâmetros de método) mantêm a lista de campos inválidos como membro de extensão `fieldErrors` (`[{ "field", "message" }]`).
- Cada categoria de erro da aplicação ganha um `type` estável no formato `urn:problem-type:<slug>`, por exemplo `urn:problem-type:resource-not-found`, e um `title` fixo.
- O `GlobalExceptionHandler` passa a estender `ResponseEntityExceptionHandler`, de modo que as exceções padrão do Spring MVC (405, 406, 415 etc.) recebam o status correto em formato Problem Details em vez de `500`.
- O mapeamento de status existente é mantido: validação/JSON malformado/tipo inválido → 400; `ResourceNotFoundException`/`EntityNotFoundException`/rota inexistente → 404; `BusinessConflictException`/`DataIntegrityViolationException` → 409; autenticação → 401; autorização → 403; não tratada → 500 sem detalhes internos.
- A documentação OpenAPI (Springdoc) passa a referenciar um schema `ProblemDetail` com media type `application/problem+json` em todas as respostas de erro, com exemplos de payload.
- Remover `common.dto.ErrorResponse`. O record `FieldErrorItem` é mantido como item de `fieldErrors`.
- Atualizar os testes (`GlobalExceptionHandlerTest`, testes de controller, `JsonSecurityHandlersTest`, `CatalogApiIntegrationTest`, `OpenApiDocsTest`) para validar os membros da RFC 9457 e o `Content-Type`.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

- `api-error-handling`: o formato padronizado passa a ser o Problem Details da RFC 9457 (`application/problem+json`, `type`/`title`/`status`/`detail`/`instance`, extensão `fieldErrors`). Os requisitos de 404, 401/403 e 500 passam a se referir a `detail`/`instance`. Entram novos requisitos para `Content-Type`, URIs de `type` e o mapeamento de erros padrão do Spring MVC.
- `api-documentation`: as respostas de erro documentadas passam a usar o schema `ProblemDetail` com media type `application/problem+json`.

## Impact

- **Clientes da API**: precisam ler `detail` em vez de `message`, `instance` em vez de `path` e `title` em vez de `error`, e aceitar `application/problem+json`. Os status HTTP não mudam, exceto os erros de framework antes devolvidos como `500` indevidamente.
- **Código**: `exception/GlobalExceptionHandler`, nova fábrica de `ProblemDetail` em `exception`, `security/SecurityErrorWriter`, `JsonAuthenticationEntryPoint`, `JsonAccessDeniedHandler`, `common/dto/ErrorResponse` (removido), anotações `@ApiResponse` em `controller/*` e `auth/AuthController`, novo schema de documentação em `common/dto`.
- **Testes**: os listados acima, além de `WebSecurityTestImports` caso o writer de segurança vire um bean.
- **Dependências**: nenhuma nova. Usa `org.springframework.http.ProblemDetail` (Spring Framework 7.0.10, Jackson 3).
