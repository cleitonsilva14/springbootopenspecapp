# Tasks

Base: `src/main/java/io/dev/openspec/springbootopenspecapp` (`<main>`), `src/test/java/io/dev/openspec/springbootopenspecapp` (`<test>`), pacote `io.dev.openspec.springbootopenspecapp` (`<base>`). O mapa completo e os imports por classe estão em `design.md` (D1–D3).

## 1. Linha de base

- [x] 1.1 Fazer backup do projeto (ou `git init` + commit inicial) e rodar `./mvnw clean verify`; registrar o total de testes executados/aprovados como referência

## 2. Pacotes de produção

- [x] 2.1 Criar `<main>/{controller,service,repository,domain,mapper,dto}` e mover os arquivos conforme D1 (`Category*`/`Product*` de `category/` e `product/`, DTOs de `category/dto/` e `product/dto/`); verificar com `find <main> -name '*.java'` que os 15 arquivos estão nos novos diretórios
- [x] 2.2 Atualizar a declaração `package` de `domain.Category` e `domain.Product` e remover de `Product` o import `<base>.category.Category`; verificar com `grep -n '^package\|^import io.dev' <main>/domain/*.java`
- [x] 2.3 Atualizar `package` dos 5 records em `<main>/dto/` para `<base>.dto`; verificar com `grep '^package' <main>/dto/*.java`
- [x] 2.4 Atualizar `package` e imports de `repository.CategoryRepository` e `repository.ProductRepository` (importar `domain.Category` / `domain.Product`) conforme D2
- [x] 2.5 Atualizar `package` e imports de `mapper.CategoryMapper` e `mapper.ProductMapper` (domain + dto) conforme D2
- [x] 2.6 Atualizar `package` e imports de `service.CategoryService` (incluindo a troca de `product.ProductRepository` por `repository.ProductRepository`) e de `service.ProductService` conforme D2, mantendo `@Service`/`@Transactional` e os membros package-private inalterados
- [x] 2.7 Atualizar `package` e imports de `controller.CategoryController` e `controller.ProductController` conforme D2, mantendo `@RestController`, `@RequestMapping`, `@Tag` e `@SecurityRequirement` inalterados
- [x] 2.8 Remover os diretórios vazios `<main>/category` e `<main>/product`; verificar com `./mvnw -q compile` (sucesso) e com `grep -rn 'springbootopenspecapp\.\(category\|product\)' src/main` (nenhum resultado)

## 3. Testes espelhados

- [x] 3.1 Criar `<test>/{controller,service,repository,mapper}` e mover os 8 testes conforme D3; verificar com `find <test> -name '*Test.java'`
- [x] 3.2 Atualizar `package` e imports de `controller.CategoryControllerTest` e `controller.ProductControllerTest` (controller/service/dto), mantendo `@WebMvcTest(...)` e `WebSecurityTestImports`
- [x] 3.3 Atualizar `package` e imports de `service.CategoryServiceTest` e `service.ProductServiceTest` (service/repository/domain/dto)
- [x] 3.4 Atualizar `package` e imports de `repository.CategoryRepositoryTest` e `repository.ProductRepositoryTest` (repository/domain)
- [x] 3.5 Atualizar `package` e imports de `mapper.CategoryMapperTest` e `mapper.ProductMapperTest` (mapper/domain/dto)
- [x] 3.6 Remover `<test>/category` e `<test>/product`; verificar com `grep -rn 'springbootopenspecapp\.\(category\|product\)' src/test` (nenhum resultado) e com `./mvnw test-compile` (sucesso)

## 4. Verificação integrada

- [x] 4.1 Rodar `./mvnw clean verify` e confirmar que a contagem de testes é igual à da linha de base (1.1), sem falhas; isso cobre `CatalogApiIntegrationTest`, `OpenApiDocsTest`, `AuthControllerTest` e os `@DataJpaTest`
- [x] 4.2 Confirmar a estrutura final com `find src -type d -path '*springbootopenspecapp*'`: existem `controller`, `service`, `repository`, `domain`, `mapper`, `dto`, e `auth`, `auth/dto`, `security`, `config`, `exception`, `common/dto` seguem intactos; não existem `category` nem `product`
