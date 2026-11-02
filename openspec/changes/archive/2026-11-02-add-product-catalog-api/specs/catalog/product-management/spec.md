# Spec Delta

## Purpose

Permite que clientes autenticados gerenciem os produtos do catálogo, cada um vinculado a uma categoria existente, com validação de preço e estoque, filtragem por categoria e paginação.

## ADDED Requirements

### Requirement: Criar produto
O sistema SHALL criar um produto via `POST /api/products` com `name` (obrigatório, máx. 150), `description` (opcional, máx. 1000), `price` (obrigatório, > 0, até 2 casas decimais), `stockQuantity` (obrigatório, >= 0) e `categoryId` (obrigatório), retornando `201 Created` e o cabeçalho `Location`.

#### Scenario: Criação com dados válidos
- **GIVEN** existe a categoria de id 1
- **WHEN** um cliente autenticado envia `POST /api/products` com `{"name": "Notebook", "price": 4500.00, "stockQuantity": 10, "categoryId": 1}`
- **THEN** o sistema responde `201 Created` com o produto criado
- **AND** o cabeçalho `Location` aponta para `/api/products/{id}`

#### Scenario: Preço não positivo
- **WHEN** um cliente autenticado cria um produto com `price` igual a 0 ou negativo
- **THEN** o sistema responde `400 Bad Request` com erro de validação no campo `price`

#### Scenario: Estoque negativo
- **WHEN** um cliente autenticado cria um produto com `stockQuantity` igual a -1
- **THEN** o sistema responde `400 Bad Request` com erro de validação no campo `stockQuantity`

#### Scenario: Campos obrigatórios ausentes
- **WHEN** um cliente autenticado cria um produto sem `name`, `price`, `stockQuantity` ou `categoryId`
- **THEN** o sistema responde `400 Bad Request` listando cada campo inválido

### Requirement: Produto exige categoria existente
O sistema MUST verificar que o `categoryId` informado na criação ou atualização de um produto corresponde a uma categoria existente e MUST responder `404 Not Found` caso contrário, sem persistir alterações.

#### Scenario: Criação com categoria inexistente
- **WHEN** um cliente autenticado cria um produto com `categoryId` 999 inexistente
- **THEN** o sistema responde `404 Not Found` com mensagem indicando categoria não encontrada
- **AND** nenhum produto é criado

#### Scenario: Atualização com categoria inexistente
- **WHEN** um cliente autenticado atualiza um produto existente informando `categoryId` 999 inexistente
- **THEN** o sistema responde `404 Not Found` e o produto permanece inalterado

### Requirement: Representação de produto inclui resumo da categoria
Toda resposta contendo produto SHALL incluir `id`, `name`, `description`, `price`, `stockQuantity`, `createdAt`, `updatedAt` e um objeto `category` com `id` e `name` da categoria associada.

#### Scenario: Resposta de consulta de produto
- **GIVEN** o produto "Notebook" pertence à categoria "Eletrônicos" de id 1
- **WHEN** um cliente autenticado consulta o produto
- **THEN** a resposta contém `"category": {"id": 1, "name": "Eletrônicos"}` e os carimbos `createdAt` e `updatedAt` em ISO-8601

### Requirement: Carimbos de data de auditoria
O sistema SHALL definir `createdAt` e `updatedAt` automaticamente na criação, MUST atualizar `updatedAt` a cada alteração e MUST ignorar valores desses campos enviados pelo cliente.

#### Scenario: Criação define carimbos
- **WHEN** um produto é criado
- **THEN** `createdAt` e `updatedAt` são preenchidos com o instante da criação

#### Scenario: Atualização altera apenas updatedAt
- **WHEN** um produto existente é atualizado
- **THEN** `updatedAt` passa a ser posterior ao valor anterior e `createdAt` permanece igual

### Requirement: Listar produtos com paginação
O sistema SHALL listar produtos via `GET /api/products` de forma paginada, aceitando `page` (padrão 0), `size` (padrão 20, máx. 100) e `sort`, retornando os itens e os metadados de paginação (número da página, tamanho, total de elementos e total de páginas).

#### Scenario: Listagem padrão
- **GIVEN** existem 25 produtos
- **WHEN** um cliente autenticado envia `GET /api/products`
- **THEN** o sistema responde `200 OK` com 20 produtos e `totalElements` igual a 25

#### Scenario: Página específica
- **GIVEN** existem 25 produtos
- **WHEN** um cliente autenticado envia `GET /api/products?page=1&size=20`
- **THEN** o sistema responde `200 OK` com 5 produtos

### Requirement: Filtrar produtos por categoria
O sistema SHALL aceitar o parâmetro opcional `categoryId` em `GET /api/products`, retornando apenas os produtos daquela categoria, e MUST responder `404 Not Found` quando a categoria filtrada não existir.

#### Scenario: Filtro por categoria existente
- **GIVEN** a categoria 1 possui 3 produtos e a categoria 2 possui 2 produtos
- **WHEN** um cliente autenticado envia `GET /api/products?categoryId=1`
- **THEN** o sistema responde `200 OK` contendo somente os 3 produtos da categoria 1

#### Scenario: Filtro por categoria inexistente
- **WHEN** um cliente autenticado envia `GET /api/products?categoryId=999` e a categoria não existe
- **THEN** o sistema responde `404 Not Found`

### Requirement: Consultar produto por id
O sistema SHALL retornar um produto via `GET /api/products/{id}` e MUST responder `404 Not Found` quando o id não existir.

#### Scenario: Produto existente
- **WHEN** um cliente autenticado envia `GET /api/products/{id}` para um id existente
- **THEN** o sistema responde `200 OK` com o produto e o resumo de sua categoria

#### Scenario: Produto inexistente
- **WHEN** um cliente autenticado envia `GET /api/products/999` e o id não existe
- **THEN** o sistema responde `404 Not Found`

### Requirement: Atualizar produto
O sistema SHALL substituir todos os campos editáveis de um produto via `PUT /api/products/{id}`, aplicando as mesmas validações da criação, e MUST responder `404 Not Found` para id inexistente.

#### Scenario: Atualização válida com troca de categoria
- **GIVEN** existem o produto de id 5 na categoria 1 e a categoria 2
- **WHEN** um cliente autenticado envia `PUT /api/products/5` com dados válidos e `categoryId` 2
- **THEN** o sistema responde `200 OK` e o produto passa a referenciar a categoria 2

#### Scenario: Atualização de produto inexistente
- **WHEN** um cliente autenticado envia `PUT /api/products/999` com dados válidos
- **THEN** o sistema responde `404 Not Found`

### Requirement: Remover produto
O sistema SHALL remover um produto via `DELETE /api/products/{id}` respondendo `204 No Content` e MUST responder `404 Not Found` para id inexistente.

#### Scenario: Remoção de produto existente
- **WHEN** um cliente autenticado envia `DELETE /api/products/{id}` para um id existente
- **THEN** o sistema responde `204 No Content` e o produto deixa de ser retornado

#### Scenario: Remoção de produto inexistente
- **WHEN** um cliente autenticado envia `DELETE /api/products/999`
- **THEN** o sistema responde `404 Not Found`
