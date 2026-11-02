# Tasks

`<main>` = `src/main/java/io/dev/openspec/springbootopenspecapp`, `<test>` = `src/test/java/io/dev/openspec/springbootopenspecapp`. As decisões D1–D6 estão em `design.md`; os cenários estão em `specs/`.

## 1. Linha de base

- [x] 1.1 Fazer backup de `src` e rodar `./mvnw clean verify`; registrar o total de testes (referência: 96, 0 falhas)

## 2. Catálogo de tipos de problema

- [x] 2.1 Criar `<main>/exception/ProblemType.java` (enum com slug, title e `HttpStatus`, com os 9 valores da tabela D2 e o método `uri()` que retorna `urn:problem-type:<slug>`)
- [x] 2.2 Criar `<main>/exception/ProblemDetails.java` com `of(ProblemType, String detail, String instance)`, que retorna um `ProblemDetail` com `type`, `title`, `status`, `detail` e `instance` preenchidos
- [x] 2.3 Criar `<test>/exception/ProblemDetailsTest.java` para verificar que cada `ProblemType` gera a URI `urn:problem-type:<slug>`, o title e o status esperados, e que `of(...)` preenche os 5 membros; verificar com `./mvnw test -Dtest=ProblemDetailsTest`

## 3. GlobalExceptionHandler em Problem Details

- [x] 3.1 Fazer `GlobalExceptionHandler` estender `ResponseEntityExceptionHandler` e criar o ponto único `respond(...)`, com a sobrescrita de `createResponseEntity(...)` que define `instance` e `Content-Type: application/problem+json` (D3)
- [x] 3.2 Sobrescrever `handleMethodArgumentNotValid` e `handleHandlerMethodValidationException` (`VALIDATION_ERROR`, `detail` "Request has N invalid field(s)", propriedade `fieldErrors` com `List<FieldErrorItem>`), além de `handleHttpMessageNotReadable` (`MALFORMED_REQUEST`) e `handleNoResourceFoundException` (`RESOURCE_NOT_FOUND`); remover os `@ExceptionHandler` equivalentes
- [x] 3.3 Converter os `@ExceptionHandler` restantes (type mismatch, autenticação, acesso negado, not found, business conflict, data integrity, genérico) para devolver `ResponseEntity<ProblemDetail>` via `ProblemDetails.of(...)`, mantendo os `log.warn`/`log.error` e os textos de `detail` da tabela D2
- [x] 3.4 Reescrever `<test>/exception/GlobalExceptionHandlerTest.java` para validar, em cada caso (400 validação, 400 JSON malformado, 400 tipo inválido, 401, 403, 404, 409 negócio, 409 integridade, 500), `Content-Type` `application/problem+json`, `type`, `title`, `status`, `detail` e `instance`. Incluir também: `fieldErrors` na raiz (e `$.properties` inexistente); ausência de `timestamp`/`error`/`message`/`path`; 500 sem nome de classe ou stack trace; 405 para método não suportado e 415 para `text/plain` com `type` `about:blank`; resposta `application/problem+json` com `Accept: application/json`. Verificar com `./mvnw test -Dtest=GlobalExceptionHandlerTest`

## 4. Erros de segurança em Problem Details

- [x] 4.1 Criar `<main>/security/ProblemDetailWriter.java` (`@Component`, mapper derivado com `ProblemDetailJacksonMixin`, método `write(request, response, ProblemType, detail)` com `application/problem+json` e UTF-8) e remover `SecurityErrorWriter.java` (D4)
- [x] 4.2 Atualizar `JsonAuthenticationEntryPoint` (`UNAUTHORIZED`, "Authentication required: provide a valid Bearer token") e `JsonAccessDeniedHandler` (`FORBIDDEN`, "Access denied") para injetar `ProblemDetailWriter`, e adicionar `ProblemDetailWriter.class` em `<test>/security/WebSecurityTestImports`
- [x] 4.3 Atualizar `<test>/security/JsonSecurityHandlersTest.java` para verificar `Content-Type` `application/problem+json`, `type`, `title`, `status` 401/403, `detail` e `instance`, e a ausência de `error`/`path`/`message`; verificar com `./mvnw test -Dtest=JsonSecurityHandlersTest`

## 5. Documentação OpenAPI

- [x] 5.1 Criar `<main>/common/dto/ProblemDetailSchema.java` (`@Schema(name = "ProblemDetail")`, campos `type`, `title`, `status`, `detail`, `instance` e `fieldErrors` com descrições e exemplos, mais a constante `MEDIA_TYPE`) conforme D5
- [x] 5.2 Trocar `ErrorResponse.class` por `ProblemDetailSchema.class` com `mediaType = ProblemDetailSchema.MEDIA_TYPE` em todas as `@ApiResponse` de `controller/CategoryController`, `controller/ProductController` e `auth/AuthController`; verificar com `grep -rn ErrorResponse src/main` (só restam as referências em `common/dto/ErrorResponse.java`)
- [x] 5.3 Atualizar `<test>/config/OpenApiDocsTest.java` para verificar que `POST /api/products` lista 400/401/404 com `$ref` `#/components/schemas/ProblemDetail` sob `application/problem+json` e que `components.schemas.ProblemDetail` tem `type`, `title`, `status`, `detail`, `instance` e `fieldErrors`; verificar com `./mvnw test -Dtest=OpenApiDocsTest`

## 6. Remoção do formato antigo e testes de controller/integração

- [x] 6.1 Remover `<main>/common/dto/ErrorResponse.java`; verificar com `grep -rn ErrorResponse src` (nenhum resultado) e com `./mvnw -q compile`
- [x] 6.2 Atualizar as asserções de erro em `<test>/controller/CategoryControllerTest.java`, `<test>/controller/ProductControllerTest.java` e `<test>/auth/AuthControllerTest.java` (`$.message` → `$.detail`, `$.path` → `$.instance`, `$.error` → `$.title`, e acrescentar `$.type` e `Content-Type`); verificar com `./mvnw test -Dtest='*ControllerTest'`
- [x] 6.3 Atualizar `<test>/CatalogApiIntegrationTest.java`: no 401 sem token, verificar `$.instance` (antes `$.path`), `$.type` e `Content-Type` `application/problem+json`; acrescentar os casos 404 com `Accept: application/json` (resposta `application/problem+json`) e `PATCH /api/categories/1` autenticado (405 em Problem Details)

## 7. Verificação integrada

- [x] 7.1 Rodar `./mvnw clean verify` e confirmar que não há falhas, com a contagem de testes ≥ a da linha de base (1.1). Isso inclui `SpringbootopenspecappApplicationTests`, que pega mapeamentos de `@ExceptionHandler` ambíguos
- [x] 7.2 Subir a aplicação (`./mvnw spring-boot:run`) e conferir com `curl -i` um 401 sem token, um 404 e um 400 de validação autenticados: `Content-Type: application/problem+json`, payloads iguais aos exemplos do `design.md` e `/v3/api-docs` com o schema `ProblemDetail`
