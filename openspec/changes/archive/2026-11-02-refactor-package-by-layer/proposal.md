# Proposal

## Why

Os módulos `category` e `product` hoje concentram, na raiz de cada pacote, classes de camadas diferentes (controller, service, repository, entidade JPA e mapper), e já há acoplamento cruzado entre eles (`CategoryService` importa `product.ProductRepository`; `Product`, `ProductService` e `ProductMapper` importam `category.Category`/`CategoryRepository`). Reorganizar em pacotes por camada (Package by Layer) deixa a responsabilidade de cada classe explícita pelo pacote, facilita localizar código e prepara a base para novas entidades sem repetir o mesmo padrão misturado.

## What Changes

Base: `io.dev.openspec.springbootopenspecapp` (abreviado como `<base>`).

- Criar os pacotes de camada e mover as classes de produção:
  - `<base>.controller`: `CategoryController`, `ProductController`
  - `<base>.service`: `CategoryService`, `ProductService`
  - `<base>.repository`: `CategoryRepository`, `ProductRepository`
  - `<base>.domain`: `Category`, `Product` (entidades JPA)
  - `<base>.mapper`: `CategoryMapper`, `ProductMapper`
  - `<base>.dto`: `CategoryRequest`, `CategoryResponse` (de `category.dto`) e `ProductRequest`, `ProductResponse`, `CategorySummary` (de `product.dto`)
- Remover os pacotes `<base>.category`, `<base>.category.dto`, `<base>.product` e `<base>.product.dto`, que ficam vazios.
- Manter inalterados: `auth` (`AuthController`) e `auth.dto` (`LoginRequest`, `LoginResponse`) — já estão no formato pedido —, além de `security`, `config`, `exception` e `common.dto`.
- Atualizar as declarações `package` e os `import` de todas as classes afetadas e de seus consumidores.
- Reorganizar os testes de `category`/`product` nos pacotes de camada espelhados (`controller`, `service`, `repository`, `mapper`) e atualizar seus imports, incluindo `CatalogApiIntegrationTest`.
- Nenhuma anotação Spring/JPA, assinatura, rota, payload JSON, regra de negócio ou configuração muda.

## Capabilities

### New Capabilities

Nenhuma.

### Modified Capabilities

Nenhuma. É uma refatoração interna sem mudança de comportamento observável; a change usa `skip_specs: true`. Os requisitos de `catalog/category-management`, `catalog/product-management`, `api-authentication`, `api-documentation` e `api-error-handling` permanecem válidos como estão.

## Impact

- **Código de produção**: 15 arquivos movidos/editados em `src/main/java` (10 de `category`/`product` e 5 DTOs). Nenhuma outra classe de produção importa esses pacotes hoje (`security`, `config`, `exception`, `common` e `auth` não precisam de mudança).
- **Testes**: 8 classes de teste em `src/test/java/.../category` e `.../product` mudam de pacote. `CatalogApiIntegrationTest` só faz chamadas HTTP e não importa classes desses pacotes, então não muda.
- **API/contrato**: sem mudança. Rotas, JSON e esquema OpenAPI continuam iguais (o Springdoc nomeia os schemas pelo nome simples da classe, que não muda).
- **Banco de dados**: sem mudança. Nomes de tabelas e colunas são explícitos em `@Table`/`@Column`, e a varredura de entidades e repositórios do `@SpringBootApplication` cobre todos os subpacotes de `<base>`.
- **Dependências**: nenhuma.
