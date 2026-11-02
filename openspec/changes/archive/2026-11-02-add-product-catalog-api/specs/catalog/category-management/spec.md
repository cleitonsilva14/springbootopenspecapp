# Spec Delta

## Purpose

Permite que clientes autenticados criem, consultem, atualizem e removam as categorias que organizam o catálogo de produtos, garantindo nomes únicos e integridade com os produtos associados.

## ADDED Requirements

### Requirement: Criar categoria
O sistema SHALL permitir criar uma categoria via `POST /api/categories` informando `name` (obrigatório, não vazio, máx. 100 caracteres) e `description` (opcional, máx. 500 caracteres), retornando `201 Created`, o cabeçalho `Location` e o corpo com `id`, `name` e `description`.

#### Scenario: Criação com dados válidos
- **WHEN** um cliente autenticado envia `POST /api/categories` com `{"name": "Eletrônicos", "description": "Aparelhos"}`
- **THEN** o sistema responde `201 Created` com o corpo contendo o `id` gerado, `name` e `description`
- **AND** o cabeçalho `Location` aponta para `/api/categories/{id}`

#### Scenario: Nome ausente ou em branco
- **WHEN** um cliente autenticado envia `POST /api/categories` com `name` vazio ou ausente
- **THEN** o sistema responde `400 Bad Request` com o erro de validação do campo `name`

### Requirement: Nome de categoria único
O sistema MUST rejeitar a criação ou atualização de uma categoria cujo `name` já pertença a outra categoria, comparando sem diferenciar maiúsculas e minúsculas.

#### Scenario: Criação com nome duplicado
- **GIVEN** existe a categoria "Eletrônicos"
- **WHEN** um cliente autenticado cria uma categoria com `name` "eletrônicos"
- **THEN** o sistema responde `409 Conflict` com mensagem indicando nome já existente

#### Scenario: Atualização para nome de outra categoria
- **GIVEN** existem as categorias "Livros" e "Eletrônicos"
- **WHEN** um cliente autenticado atualiza "Livros" com `name` "Eletrônicos"
- **THEN** o sistema responde `409 Conflict`

#### Scenario: Atualização mantendo o próprio nome
- **GIVEN** existe a categoria "Livros"
- **WHEN** um cliente autenticado atualiza "Livros" mantendo `name` "Livros" e alterando `description`
- **THEN** o sistema responde `200 OK` com a descrição atualizada

### Requirement: Listar categorias
O sistema SHALL listar todas as categorias via `GET /api/categories`, retornando `200 OK` com uma lista ordenada por `name`.

#### Scenario: Listagem com categorias cadastradas
- **GIVEN** existem as categorias "Livros" e "Eletrônicos"
- **WHEN** um cliente autenticado envia `GET /api/categories`
- **THEN** o sistema responde `200 OK` com as duas categorias, "Eletrônicos" antes de "Livros"

#### Scenario: Listagem vazia
- **WHEN** um cliente autenticado envia `GET /api/categories` sem categorias cadastradas
- **THEN** o sistema responde `200 OK` com uma lista vazia

### Requirement: Consultar categoria por id
O sistema SHALL retornar uma categoria via `GET /api/categories/{id}` e MUST responder `404 Not Found` quando o id não existir.

#### Scenario: Categoria existente
- **WHEN** um cliente autenticado envia `GET /api/categories/{id}` para um id existente
- **THEN** o sistema responde `200 OK` com `id`, `name` e `description`

#### Scenario: Categoria inexistente
- **WHEN** um cliente autenticado envia `GET /api/categories/999` e o id não existe
- **THEN** o sistema responde `404 Not Found` com mensagem indicando categoria não encontrada

### Requirement: Atualizar categoria
O sistema SHALL substituir `name` e `description` de uma categoria via `PUT /api/categories/{id}`, aplicando as mesmas validações da criação, e MUST responder `404 Not Found` para id inexistente.

#### Scenario: Atualização válida
- **WHEN** um cliente autenticado envia `PUT /api/categories/{id}` com dados válidos para um id existente
- **THEN** o sistema responde `200 OK` com a categoria atualizada

#### Scenario: Atualização de categoria inexistente
- **WHEN** um cliente autenticado envia `PUT /api/categories/999` e o id não existe
- **THEN** o sistema responde `404 Not Found`

### Requirement: Remover categoria
O sistema SHALL remover uma categoria via `DELETE /api/categories/{id}` respondendo `204 No Content`, MUST responder `404 Not Found` para id inexistente e MUST recusar com `409 Conflict` a remoção de uma categoria que possua produtos associados.

#### Scenario: Remoção de categoria sem produtos
- **WHEN** um cliente autenticado envia `DELETE /api/categories/{id}` para uma categoria sem produtos
- **THEN** o sistema responde `204 No Content` e a categoria deixa de ser retornada nas consultas

#### Scenario: Remoção de categoria com produtos
- **GIVEN** a categoria possui ao menos um produto associado
- **WHEN** um cliente autenticado envia `DELETE /api/categories/{id}`
- **THEN** o sistema responde `409 Conflict` e a categoria permanece cadastrada

#### Scenario: Remoção de categoria inexistente
- **WHEN** um cliente autenticado envia `DELETE /api/categories/999` e o id não existe
- **THEN** o sistema responde `404 Not Found`
