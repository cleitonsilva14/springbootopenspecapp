# Design

## Context

O repositório contém apenas o esqueleto do Spring Initializr: `pom.xml` com **Spring Boot 4.1.2**, `java.version` 21, `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-h2console`, H2 (runtime), Lombok, devtools e `springdoc-openapi-starter-webmvc-ui` 3.2.0; os starters de teste `spring-boot-starter-data-jpa-test` e `spring-boot-starter-webmvc-test`. Há só a classe `SpringbootopenspecappApplication` e o teste `contextLoads`. Pacote base: `io.dev.openspec.springbootopenspecapp`. Faltam Security, Validation e JJWT. Motivação em proposal.md; requisitos nas specs desta mudança.

Restrições relevantes do Spring Boot 4 que moldam o desenho:
- Starters modulares (`spring-boot-starter-webmvc`, `spring-boot-starter-security`, `spring-boot-starter-validation`) e starters de teste correspondentes (`spring-boot-starter-security-test`).
- Anotações de slice de teste mudaram de pacote (ex.: `@WebMvcTest` em `org.springframework.boot.webmvc.test.autoconfigure`) e `@MockBean` foi removido em favor de `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`).
- Jackson 3 (`tools.jackson.*`) é o padrão da aplicação; o JJWT usa Jackson 2 internamente via `jjwt-jackson`, o que é isolado (só serializa claims).

## Goals / Non-Goals

**Goals:**
- Arquitetura em camadas clara (controller → service → repository) com DTOs isolando a API das entidades JPA.
- Segurança stateless com JWT, simples de configurar por variáveis de ambiente.
- Formato de erro único para todas as falhas, inclusive as originadas na cadeia de filtros do Security.
- Testes unitários rápidos para services (Mockito puro) e controllers (`@WebMvcTest`).

**Non-Goals:**
- Persistência de usuários em banco ou endpoints de cadastro de usuário.
- Refresh token, revogação/blacklist de tokens, RBAC por papel.
- Migrações versionadas (Flyway/Liquibase) e perfil PostgreSQL — o schema é gerado pelo Hibernate sobre H2.
- Cache, rate limiting, HATEOAS.

## Decisions

### 1. Estrutura de pacotes por camada
```
io.dev.openspec.springbootopenspecapp
├── config/        OpenApiConfig, (JpaConfig se necessário)
├── security/      SecurityConfig, JwtService, JwtAuthenticationFilter,
│                  JsonAuthenticationEntryPoint, JsonAccessDeniedHandler,
│                  AppUserProperties
├── auth/          AuthController, dto/LoginRequest, dto/LoginResponse
├── category/      Category, CategoryRepository, CategoryService,
│                  CategoryController, CategoryMapper, dto/*
├── product/       Product, ProductRepository, ProductService,
│                  ProductController, ProductMapper, dto/*
├── common/dto/    PageResponse<T>, ErrorResponse, FieldErrorItem
└── exception/     ResourceNotFoundException, BusinessConflictException,
                   GlobalExceptionHandler
```
**Por quê:** agrupar por feature (category/product) dentro de uma aplicação pequena mantém coesão e evita pacotes `controller/` e `service/` inchados. *Alternativa:* pacotes por camada técnica pura (`controller`, `service`, ...) — também atende ao pedido; descartada por espalhar cada feature em vários diretórios. Dentro de cada feature as camadas MVC permanecem explícitas.

### 2. Identificadores `Long` com `IDENTITY`
Product e Category usam `Long` com `GenerationType.IDENTITY`. **Por quê:** consistência entre as duas entidades e URLs simples; o requisito aceitava Long ou UUID. *Alternativa:* UUID para Product — útil contra enumeração de ids, mas sem benefício real aqui já que toda a API exige autenticação.

### 3. Entidades JPA
- `Category`: `name` com `@Column(nullable=false, unique=true, length=100)`, `description` `length=500`.
- `Product`: `price` `BigDecimal` `precision=12, scale=2`; `stockQuantity` `Integer` not null; `@ManyToOne(fetch = LAZY, optional = false) @JoinColumn(name="category_id")`; `createdAt`/`updatedAt` como `Instant` com `@CreationTimestamp` e `@UpdateTimestamp` (Hibernate). **Por quê:** dispensa configurar `@EnableJpaAuditing`/`AuditorAware` para algo que não precisa de "quem alterou". *Alternativa:* Spring Data JPA Auditing — mais flexível, desnecessário agora.
- Lombok apenas `@Getter/@Setter/@NoArgsConstructor` nas entidades (sem `@Data`, para evitar `equals/hashCode/toString` com relacionamentos lazy).
- Sem relacionamento `@OneToMany` em Category: a verificação "categoria tem produtos" usa `productRepository.existsByCategoryId(id)`, evitando carregar coleções.

### 4. Unicidade de nome case-insensitive
`CategoryRepository.existsByNameIgnoreCase(name)` e `existsByNameIgnoreCaseAndIdNot(name, id)` no service geram `BusinessConflictException` (409). A constraint `unique` do banco é a rede de segurança; uma `DataIntegrityViolationException` também é mapeada para 409 no handler. *Alternativa:* só constraint do banco — falha para variação de caixa e gera mensagens pouco amigáveis.

### 5. DTOs como records + mappers manuais
`CategoryRequest(name, description)`, `CategoryResponse(id, name, description)`, `ProductRequest(name, description, price, stockQuantity, categoryId)`, `ProductResponse(id, name, description, price, stockQuantity, category: CategorySummary(id, name), createdAt, updatedAt)`. Validações: `@NotBlank @Size`, `@NotNull @Positive @Digits(integer=10, fraction=2)` em price, `@NotNull @PositiveOrZero` em stockQuantity, `@NotNull` em categoryId. Mappers são componentes estáticos/simples (`CategoryMapper`, `ProductMapper`). **Por quê:** poucas classes, zero geração de código. *Alternativa:* MapStruct — adiciona annotation processor e conflito de ordem com Lombok sem ganho proporcional. Nomes dos DTOs seguem `*Request/*Response` (equivalentes aos `ProductRequestDTO/ProductResponseDTO` do pedido).

### 6. Paginação com DTO próprio
`GET /api/products` recebe `Pageable` (`@PageableDefault(size = 20, sort = "name")`) e o limite de 100 é aplicado via `spring.data.web.pageable.max-page-size=100`. A resposta é `PageResponse<T>(content, page, size, totalElements, totalPages)`. **Por quê:** serializar `PageImpl` diretamente gera aviso do Spring Data e um JSON instável. *Alternativa:* `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` — válido, mas o DTO próprio dá contrato explícito no OpenAPI. A consulta usa `@EntityGraph(attributePaths = "category")` em `findAll(Pageable)` e `findByCategoryId(Long, Pageable)` para evitar N+1.

### 7. Services transacionais
`@Transactional(readOnly = true)` na classe e `@Transactional` nos métodos de escrita. `ProductService` resolve a categoria por `categoryRepository.findById(...)` lançando `ResourceNotFoundException("Category", id)` antes de persistir. O filtro por `categoryId` valida existência da categoria (404) antes de consultar. `spring.jpa.open-in-view=false`, por isso o mapeamento para DTO ocorre dentro do service.

### 8. Segurança JWT
- **Usuários:** `InMemoryUserDetailsManager` montado a partir de `AppUserProperties` (`app.security.user.username`/`password`, com padrão `admin`/senha de dev, sobrescrevíveis por `APP_SECURITY_USER_*`). A senha é codificada com `BCryptPasswordEncoder` na inicialização. **Por quê:** atende "credenciais" sem introduzir tabela e endpoints de usuário (fora de escopo). *Alternativa:* entidade `User` + `UserRepository` — pronta para evoluir, porém amplia escopo; a troca futura só substitui o bean `UserDetailsService`.
- **Login:** `AuthController` chama `AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken)`; `BadCredentialsException` vira 401 genérico no handler.
- **JwtService:** JJWT 0.12.x (`Jwts.builder().subject().issuedAt().expiration().signWith(SecretKey)`), chave via `Keys.hmacShaKeyFor(Base64.decode(app.security.jwt.secret))`, exigindo ≥ 256 bits (falha na inicialização caso contrário). Expiração `app.security.jwt.expiration` (`Duration`, padrão `1h`).
- **Filtro:** `JwtAuthenticationFilter extends OncePerRequestFilter`, lê `Authorization: Bearer`, valida assinatura/expiração e define `UsernamePasswordAuthenticationToken` no `SecurityContextHolder`. Token inválido: limpa o contexto e deixa a cadeia seguir; o `AuthenticationEntryPoint` responde 401. Não consulta o `UserDetailsService` a cada requisição (o token é a fonte de verdade — stateless).
- **SecurityFilterChain:** `csrf().disable()` (API sem cookies), `sessionManagement(STATELESS)`, `permitAll` para `/api/auth/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`, `/error`; `authenticated()` para `/api/categories/**`, `/api/products/**` e `anyRequest()`. `httpBasic`/`formLogin` desabilitados. Entry point e access-denied handler customizados escrevem o `ErrorResponse` em JSON.
- *Alternativa:* `spring-boot-starter-oauth2-resource-server` com `NimbusJwtDecoder` — mais padrão para validação, mas o requisito pede explicitamente JJWT e filtro próprio.

### 9. Tratamento de erros
`ErrorResponse(timestamp, status, error, message, path, fieldErrors)` com `@JsonInclude(NON_EMPTY)` para `fieldErrors`. `GlobalExceptionHandler` (`@RestControllerAdvice`):
| Exceção | Status |
|---|---|
| `MethodArgumentNotValidException`, `HandlerMethodValidationException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` | 400 |
| `BadCredentialsException` / `AuthenticationException` | 401 |
| `ResourceNotFoundException` (própria) e `jakarta.persistence.EntityNotFoundException` | 404 |
| `BusinessConflictException`, `DataIntegrityViolationException` | 409 |
| `Exception` | 500 (mensagem genérica, log `error` com stack trace) |

**Por quê formato próprio e não `ProblemDetail`:** a spec define campos fixos (`timestamp`, `path`, `fieldErrors`) que o cliente consome; `ProblemDetail` exigiria propriedades extras de qualquer forma. A exceção própria `ResourceNotFoundException` é usada nos services; o handler também cobre `EntityNotFoundException` por compatibilidade com `getReferenceById`.

### 10. OpenAPI
`OpenApiConfig` declara um bean `OpenAPI` com `Info` (título, versão, descrição) e `SecurityScheme` `bearerAuth` (`HTTP`, `bearer`, `JWT`). Controllers de catálogo recebem `@SecurityRequirement(name = "bearerAuth")` em nível de classe; `AuthController` não. Operações anotadas com `@Operation`/`@ApiResponse` incluindo os códigos de erro e o schema `ErrorResponse`. Swagger UI em `/swagger-ui/index.html` (padrão do Springdoc).

### 11. Configuração (`application.properties`)
H2 em memória (`jdbc:h2:mem:catalog`), `spring.jpa.hibernate.ddl-auto=update` (dev) , `open-in-view=false`, `spring.h2.console.enabled=true` só no perfil `dev`, propriedades `app.security.jwt.secret`/`expiration` e `app.security.user.*` com placeholders `${ENV:default}`. Perfil de teste reutiliza H2.

### 12. Estratégia de testes
- **Services:** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), sem contexto Spring.
- **Controllers:** `@WebMvcTest(XController.class)` + `@Import(SecurityConfig, JwtAuthenticationFilter, JwtService, handlers)` e `@MockitoBean` para services; requisições autenticadas com `SecurityMockMvcRequestPostProcessors.user(...)`/`@WithMockUser` e cenários 401 sem usuário. Validação, 404 e 409 testados via MockMvc.
- **Segurança:** testes unitários do `JwtService` (gerar/validar/expirado/assinatura inválida) e teste de integração `@SpringBootTest` + `@AutoConfigureMockMvc` cobrindo login → uso do token → 401 sem token.
- `contextLoads` continua válido.

## Risks / Trade-offs

- [Segredo JWT padrão em `application.properties` vazar para produção] → valor padrão apenas para dev, documentado; `JwtService` valida o tamanho mínimo e o README/HELP instrui definir `APP_SECURITY_JWT_SECRET`.
- [Usuário em memória não escala para múltiplos usuários] → `UserDetailsService` isolado em um bean, substituível por implementação JPA sem tocar filtro/controller.
- [Tokens não revogáveis antes da expiração] → expiração curta (1h) e configurável; revogação fora de escopo.
- [APIs de teste do Spring Boot 4 diferentes das do 3.x (pacotes, `@MockitoBean`)] → tasks referenciam explicitamente os starters de teste do Boot 4; compilar cedo para detectar divergências.
- [Compatibilidade JJWT/Jackson 2 com Boot 4 em Jackson 3] → `jjwt-jackson` traz `jackson-databind` 2 apenas como dependência transitiva isolada; se houver conflito, usar `jjwt-gson` como alternativa.
- [`ddl-auto=update` sobre H2 não é estratégia de produção] → aceito nesta mudança (non-goal); migrações ficam para mudança futura.
- [Race condition na checagem de nome único] → constraint `unique` no banco + mapeamento de `DataIntegrityViolationException` para 409.

## Migration Plan

Aplicação nova, sem dados existentes. Deploy: definir `APP_SECURITY_JWT_SECRET` (Base64, ≥ 32 bytes) e `APP_SECURITY_USER_PASSWORD`, executar `./mvnw clean verify` e `./mvnw spring-boot:run`. Rollback: reverter o commit; não há migração de dados.
