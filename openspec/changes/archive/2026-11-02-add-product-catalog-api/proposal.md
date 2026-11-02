# Proposal

## Why

O projeto `springbootopenspecapp` é hoje apenas o esqueleto gerado pelo Spring Initializr (Spring Boot 4.1.2, Java 21, H2, JPA, WebMVC, Springdoc), sem nenhuma funcionalidade de negócio. É necessário entregar uma API RESTful pronta para produção que permita gerenciar um catálogo de **Produtos** e **Categorias**, protegida por autenticação JWT stateless e documentada via Swagger, para servir de base para clientes (front-end, integrações) consumirem o catálogo de forma segura.

## What Changes

- Adicionar ao `pom.xml` as dependências ausentes: `spring-boot-starter-security`, `spring-boot-starter-validation`, `jjwt-api`, `jjwt-impl`, `jjwt-jackson` e as de teste de segurança (`spring-security-test`). Manter as existentes (WebMVC, Data JPA, H2, Lombok, Springdoc 3.2.0).
- Criar o domínio JPA `Category` (id, name único e obrigatório, description) e `Product` (id, name obrigatório, description, price positivo, stockQuantity não-negativo, createdAt, updatedAt, `@ManyToOne` com Category).
- Criar `CategoryRepository` e `ProductRepository` (estendendo `JpaRepository`), com consulta de produtos por categoria.
- Criar DTOs (records) de Request/Response com anotações `jakarta.validation`; `ProductRequestDTO` recebe `categoryId` e `ProductResponseDTO` inclui um resumo da categoria; mappers dedicados.
- Criar `CategoryService` e `ProductService` com regras de negócio (categoria deve existir ao associar produto; nome de categoria único; categoria com produtos não pode ser removida).
- Criar `CategoryController` (`/api/categories`) e `ProductController` (`/api/products`) com CRUD completo e filtro de produtos por `categoryId`, com paginação.
- Criar a camada de segurança: `SecurityConfig` stateless, endpoint `POST /api/auth/login` que emite JWT, filtro JWT que valida o token e popula o `SecurityContext`, serviço de geração/validação de tokens com segredo e expiração configuráveis.
- Proteger `/api/products/**` e `/api/categories/**` (exigem JWT válido); liberar `/api/auth/**`, Swagger UI e `/v3/api-docs/**`.
- Configurar Springdoc OpenAPI com esquema de segurança Bearer (JWT) para testes pela Swagger UI em `/swagger-ui/index.html`.
- Criar handler global (`@RestControllerAdvice`) com resposta de erro padronizada para validação (400), recurso não encontrado (404), conflito (409), falha de autenticação (401) e erros genéricos (500).
- Criar testes unitários para services (Mockito) e controllers (`@WebMvcTest`), além de testes do fluxo de autenticação.

## Capabilities

### New Capabilities
- `catalog/category-management`: CRUD de categorias, unicidade de nome e regras de remoção.
- `catalog/product-management`: CRUD de produtos, validação de preço/estoque, associação obrigatória a uma categoria existente, filtragem por categoria e paginação.
- `api-authentication`: emissão de JWT via `/api/auth/login`, validação de token em cada requisição e proteção dos endpoints do catálogo.
- `api-documentation`: exposição da especificação OpenAPI e da Swagger UI com suporte a autenticação Bearer.
- `api-error-handling`: formato padronizado de respostas de erro e mapeamento de exceções para códigos HTTP.

### Modified Capabilities
<!-- Nenhuma: não existem specs anteriores em openspec/specs/. -->

## Impact

- **Código**: novos pacotes em `io.dev.openspec.springbootopenspecapp` (`domain`, `repository`, `dto`, `mapper`, `service`, `controller`, `security`, `config`, `exception`); `application.properties` ganha configuração de datasource, JPA, JWT e Springdoc.
- **APIs**: novos endpoints `/api/auth/login`, `/api/categories/**`, `/api/products/**`; todos os endpoints do catálogo passam a exigir `Authorization: Bearer <token>`.
- **Dependências**: Spring Security, Bean Validation e JJWT adicionados ao build.
- **Testes**: o teste existente `contextLoads` precisa continuar passando com a segurança ativa.
- **Fora de escopo**: cadastro/gestão de usuários via API, refresh tokens, autorização por papéis (RBAC) granular, migrações com Flyway/Liquibase, perfil PostgreSQL de produção (apenas H2 nesta mudança; a troca de driver fica documentada como evolução).
