# api-error-handling Specification

## Purpose

Garante que todas as falhas da API sejam devolvidas em um formato JSON único e previsível, com o código HTTP adequado e sem expor detalhes internos do servidor.

## Requirements

### Requirement: Formato padronizado de erro
Toda resposta de erro da API SHALL ter corpo JSON com `timestamp` (ISO-8601), `status` (código HTTP), `error` (descrição do status), `message` (mensagem legível) e `path` (caminho requisitado); erros de validação MUST incluir também `fieldErrors`, lista de objetos com `field` e `message`.

#### Scenario: Estrutura de um erro 404
- **WHEN** um cliente autenticado consulta `GET /api/categories/999` inexistente
- **THEN** o corpo da resposta contém `timestamp`, `status` 404, `error` "Not Found", `message` e `path` "/api/categories/999"

### Requirement: Erros de validação retornam 400
O sistema MUST responder `400 Bad Request` quando o corpo da requisição violar regras de validação, informando em `fieldErrors` todos os campos inválidos de uma só vez, e também quando o corpo JSON for malformado ou um parâmetro tiver tipo inválido.

#### Scenario: Múltiplos campos inválidos
- **WHEN** um cliente autenticado cria um produto com `name` vazio e `price` negativo
- **THEN** o sistema responde `400 Bad Request` e `fieldErrors` contém entradas para `name` e `price`

#### Scenario: JSON malformado
- **WHEN** um cliente autenticado envia `POST /api/categories` com corpo JSON sintaticamente inválido
- **THEN** o sistema responde `400 Bad Request` no formato padronizado

#### Scenario: Parâmetro de caminho com tipo inválido
- **WHEN** um cliente autenticado envia `GET /api/products/abc`
- **THEN** o sistema responde `400 Bad Request` no formato padronizado

### Requirement: Recurso inexistente retorna 404
O sistema MUST responder `404 Not Found` quando uma categoria ou produto referenciado não existir, com `message` identificando o tipo de recurso e o id procurado.

#### Scenario: Categoria referenciada inexistente
- **WHEN** um cliente autenticado cria um produto com `categoryId` 42 inexistente
- **THEN** o sistema responde `404 Not Found` e `message` menciona a categoria de id 42

### Requirement: Violação de regra de negócio retorna 409
O sistema MUST responder `409 Conflict` quando a operação violar uma regra de integridade de negócio, como nome de categoria duplicado ou remoção de categoria com produtos.

#### Scenario: Conflito de nome
- **WHEN** um cliente autenticado cria uma categoria com nome já existente
- **THEN** o sistema responde `409 Conflict` no formato padronizado

### Requirement: Erros de autenticação no formato padronizado
O sistema MUST devolver falhas de autenticação (`401 Unauthorized`) e de autorização (`403 Forbidden`) no mesmo formato padronizado de erro.

#### Scenario: Acesso sem token
- **WHEN** um cliente envia `GET /api/products` sem token
- **THEN** o sistema responde `401 Unauthorized` com corpo contendo `status` 401 e `path` "/api/products"

### Requirement: Erro inesperado retorna 500 sem detalhes internos
O sistema MUST responder `500 Internal Server Error` para exceções não tratadas, com `message` genérica, e MUST NOT expor stack trace, nomes de classes ou mensagens SQL na resposta, registrando o detalhe apenas no log do servidor.

#### Scenario: Falha inesperada
- **WHEN** ocorre uma exceção não prevista durante o processamento de uma requisição autenticada
- **THEN** o sistema responde `500 Internal Server Error` com `message` genérica e sem stack trace no corpo
