# Design

## Context

Motivação: ver `proposal.md` (seção Why).

Estado atual (base `io.dev.openspec.springbootopenspecapp`, abreviada `<base>`):

- `<base>.category`: `Category`, `CategoryController`, `CategoryMapper`, `CategoryRepository`, `CategoryService`; `<base>.category.dto`: `CategoryRequest`, `CategoryResponse`.
- `<base>.product`: `Product`, `ProductController`, `ProductMapper`, `ProductRepository`, `ProductService`; `<base>.product.dto`: `CategorySummary`, `ProductRequest`, `ProductResponse`.
- `<base>.auth.AuthController` e `<base>.auth.dto.{LoginRequest,LoginResponse}` já estão separados por camada.
- `SpringbootopenspecappApplication` fica em `<base>`, então a varredura de componentes, entidades JPA e repositórios do Spring Data cobre qualquer subpacote. Não há `@EntityScan`, `@EnableJpaRepositories` ou `scanBasePackages` explícitos.
- Membros package-private: `CategoryService.RESOURCE` e `CategoryService.getCategory(Long)` são usados apenas dentro da própria classe. Todas as demais classes e DTOs (records) são `public`. Nenhum teste depende de acesso package-private.
- O projeto não é um repositório git; os arquivos são movidos no sistema de arquivos (não há `git mv`).

## Goals / Non-Goals

**Goals:**
- Cada classe de `category`/`product` passa a viver no pacote da sua camada, sem mudar comportamento.
- `./mvnw clean verify` compila e passa com a mesma suíte de testes.

**Non-Goals:**
- Renomear classes (por exemplo, sufixo `DTO`/`Entity`), mudar visibilidade, assinaturas ou anotações.
- Mover `auth`, `auth.dto`, `security`, `config`, `exception` ou `common.dto`.
- Mover `AuthController` para `controller` (o pedido mantém `auth` como pacote próprio).
- Introduzir regras de arquitetura automatizadas (ArchUnit etc.).

## Decisions

### D1. Mapa de movimentação (produção)

| Classe | De | Para |
|---|---|---|
| `CategoryController` | `<base>.category` | `<base>.controller` |
| `ProductController` | `<base>.product` | `<base>.controller` |
| `CategoryService` | `<base>.category` | `<base>.service` |
| `ProductService` | `<base>.product` | `<base>.service` |
| `CategoryRepository` | `<base>.category` | `<base>.repository` |
| `ProductRepository` | `<base>.product` | `<base>.repository` |
| `Category` | `<base>.category` | `<base>.domain` |
| `Product` | `<base>.product` | `<base>.domain` |
| `CategoryMapper` | `<base>.category` | `<base>.mapper` |
| `ProductMapper` | `<base>.product` | `<base>.mapper` |
| `CategoryRequest`, `CategoryResponse` | `<base>.category.dto` | `<base>.dto` |
| `ProductRequest`, `ProductResponse`, `CategorySummary` | `<base>.product.dto` | `<base>.dto` |
| `AuthController`, `LoginRequest`, `LoginResponse` | `<base>.auth`, `<base>.auth.dto` | sem mudança |

**Entidades em `domain`, não em `model`/`entity`:** o pedido aceita qualquer um deles, e `domain` é a primeira opção listada. O nome não afeta o comportamento.

**DTOs concentrados em `<base>.dto` (sem subpacotes):** com a organização por camada, manter `category.dto`/`product.dto` deixaria pacotes `category`/`product` contendo só DTOs, ou seja, o mesmo agrupamento por feature que esta change remove. Não há colisão de nomes simples entre os cinco records. *Alternativa considerada:* `dto.category`/`dto.product`, que acrescenta um nível sem ganho para cinco classes. `common.dto` e `auth.dto` continuam como estão, porque são compartilhados ou pertencem a outro módulo e ficam fora do escopo.

### D2. Atualização de `package` e `import` por classe

Os imports na mesma camada (ex.: `CategoryService` → `ProductRepository`) passam a ser imports entre pacotes. Classes que antes estavam no mesmo pacote e eram referenciadas sem import agora precisam de import explícito.

- `controller.CategoryController`: importar `service.CategoryService`, `dto.CategoryRequest`, `dto.CategoryResponse`. Os imports de `common.dto.ErrorResponse` e `config.OpenApiConfig` permanecem.
- `controller.ProductController`: importar `service.ProductService`, `dto.ProductRequest`, `dto.ProductResponse`. Os imports de `common.dto.*` e `config.OpenApiConfig` permanecem.
- `service.CategoryService`: importar `domain.Category`, `repository.CategoryRepository`, `repository.ProductRepository` (era `product.ProductRepository`), `mapper.CategoryMapper`, `dto.CategoryRequest`, `dto.CategoryResponse`. Os imports de `exception.*` permanecem.
- `service.ProductService`: importar `domain.Category`, `domain.Product`, `repository.CategoryRepository`, `repository.ProductRepository`, `mapper.ProductMapper`, `dto.ProductRequest`, `dto.ProductResponse`. Os imports de `common.dto.PageResponse` e `exception.ResourceNotFoundException` permanecem.
- `repository.CategoryRepository`: importar `domain.Category`.
- `repository.ProductRepository`: importar `domain.Product`.
- `domain.Category`: só muda o `package`.
- `domain.Product`: remover o import `category.Category`, que passa a estar no mesmo pacote.
- `mapper.CategoryMapper`: importar `domain.Category`, `dto.CategoryRequest`, `dto.CategoryResponse`.
- `mapper.ProductMapper`: importar `domain.Category`, `domain.Product`, `dto.CategorySummary`, `dto.ProductRequest`, `dto.ProductResponse`.
- `dto.*` (5 records): só muda o `package`.

`CategoryService.RESOURCE` e `getCategory` continuam package-private, porque só são usados internamente. Mudar a visibilidade está fora do escopo.

### D3. Testes espelham a camada

| Teste | De | Para |
|---|---|---|
| `CategoryControllerTest`, `ProductControllerTest` | `category`, `product` | `controller` |
| `CategoryServiceTest`, `ProductServiceTest` | `category`, `product` | `service` |
| `CategoryRepositoryTest`, `ProductRepositoryTest` | `category`, `product` | `repository` |
| `CategoryMapperTest`, `ProductMapperTest` | `category`, `product` | `mapper` |

Cada teste recebe imports das classes de produção que antes estavam no mesmo pacote, e os imports antigos (`category.*`, `product.*`, `category.dto.*`, `product.dto.*`) são trocados pelos novos. `@WebMvcTest(XController.class)` e `@DataJpaTest` continuam encontrando `@SpringBootConfiguration` subindo a hierarquia de pacotes até `<base>`. `CatalogApiIntegrationTest`, `AuthControllerTest` e os testes de `security`/`config`/`exception`/`common` não mudam.

### D4. Execução

Primeiro, registrar uma linha de base com `./mvnw clean verify`. Depois, mover os arquivos com `mkdir -p` + `mv` e editar `package`/imports. As classes de produção se referenciam em ciclo (domain ↔ service ↔ repository), então o código de produção só volta a compilar depois que todas as 15 classes estiverem atualizadas: o checkpoint é `./mvnw -q compile` ao fim desse bloco. Em seguida vêm os testes, com checkpoint em `./mvnw test`. Os imports são escritos explicitamente (sem curingas `.*`), no mesmo estilo do código atual.

## Risks / Trade-offs

- [Import esquecido de classe que antes estava no mesmo pacote] → o compilador acusa. Rodar `./mvnw test-compile` ao final de cada grupo de tarefas e `grep` por `springbootopenspecapp.category`/`.product` para confirmar que não sobrou referência.
- [Diretórios antigos vazios esquecidos] → remover `category/`, `product/` (main e test) e verificar com `find`.
- [Mudança no esquema OpenAPI ou no banco] → improvável, porque os nomes simples das classes e o `@Table`/`@Column` não mudam. `OpenApiDocsTest`, `CatalogApiIntegrationTest` e os `@DataJpaTest` cobrem isso.
- [Sem git para rollback] → o rollback é refazer os `mv` no sentido inverso. Recomenda-se fazer um backup ou `git init` + commit antes de aplicar.

## Migration Plan

Não há migração de dados nem de API. O deploy é o mesmo artefato com as classes reorganizadas. O rollback é restaurar a estrutura anterior a partir de um backup.
