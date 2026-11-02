## MODIFIED Requirements

### Requirement: Formato padronizado de erro
Toda resposta de erro da API (status 4xx e 5xx) SHALL ter corpo no formato Problem Details da RFC 9457, contendo `type` (URI do tipo de problema), `title` (resumo do tipo), `status` (código HTTP), `detail` (explicação específica da ocorrência) e `instance` (caminho da requisição). Erros de validação MUST incluir também o membro de extensão `fieldErrors`, lista de objetos com `field` e `message`. Os campos `timestamp`, `error`, `message` e `path` MUST NOT aparecer.

#### Scenario: Estrutura de um erro 404
- **WHEN** um cliente autenticado consulta `GET /api/categories/999` inexistente
- **THEN** o corpo da resposta contém `type` "urn:problem-type:resource-not-found", `title` "Resource not found", `status` 404, `detail` "Category with id 999 not found" e `instance` "/api/categories/999"
- **AND** o corpo não contém `timestamp`, `error`, `message` nem `path`

#### Scenario: Estrutura de um erro de validação
- **WHEN** um cliente autenticado cria uma categoria com `name` vazio
- **THEN** o corpo da resposta contém `type` "urn:problem-type:validation-error", `title`, `status` 400, `detail`, `instance` "/api/categories" e `fieldErrors` com uma entrada cujo `field` é "name"

### Requirement: Recurso inexistente retorna 404
O sistema MUST responder `404 Not Found` quando uma categoria ou produto referenciado não existir, com `detail` identificando o tipo de recurso e o id procurado.

#### Scenario: Categoria referenciada inexistente
- **WHEN** um cliente autenticado cria um produto com `categoryId` 42 inexistente
- **THEN** o sistema responde `404 Not Found` e `detail` menciona a categoria de id 42

### Requirement: Erros de autenticação no formato padronizado
O sistema MUST devolver falhas de autenticação (`401 Unauthorized`) e de autorização (`403 Forbidden`) no mesmo formato Problem Details, inclusive quando a falha é detectada pela cadeia de filtros de segurança antes de chegar a um controller.

#### Scenario: Acesso sem token
- **WHEN** um cliente envia `GET /api/products` sem token
- **THEN** o sistema responde `401 Unauthorized` com `Content-Type` `application/problem+json`, `type` "urn:problem-type:unauthorized", `status` 401 e `instance` "/api/products"

#### Scenario: Acesso negado
- **WHEN** um cliente autenticado sem permissão acessa um recurso protegido
- **THEN** o sistema responde `403 Forbidden` com `type` "urn:problem-type:forbidden" e `status` 403

#### Scenario: Credenciais inválidas no login
- **WHEN** o cliente envia `POST /api/auth/login` com senha incorreta
- **THEN** o sistema responde `401 Unauthorized` com `type` "urn:problem-type:unauthorized" e `detail` genérico "Invalid username or password"

### Requirement: Erro inesperado retorna 500 sem detalhes internos
O sistema MUST responder `500 Internal Server Error` para exceções não tratadas, com `detail` genérico, e MUST NOT expor stack trace, nomes de classes ou mensagens SQL na resposta, registrando o detalhe apenas no log do servidor.

#### Scenario: Falha inesperada
- **WHEN** ocorre uma exceção não prevista durante o processamento de uma requisição autenticada
- **THEN** o sistema responde `500 Internal Server Error` com `type` "urn:problem-type:internal-error", `detail` "An unexpected error occurred" e sem stack trace no corpo

## ADDED Requirements

### Requirement: Content-Type application/problem+json
Toda resposta de erro SHALL ser enviada com `Content-Type: application/problem+json`, independentemente de o cliente ter enviado `Accept` ausente, `*/*` ou `application/json`.

#### Scenario: Cliente aceita apenas application/json
- **WHEN** um cliente autenticado envia `GET /api/products/999` com `Accept: application/json`
- **THEN** o sistema responde `404 Not Found` com `Content-Type` `application/problem+json`

#### Scenario: Cliente sem cabeçalho Accept
- **WHEN** um cliente envia `GET /api/categories` sem token e sem cabeçalho `Accept`
- **THEN** o sistema responde `401 Unauthorized` com `Content-Type` `application/problem+json`

### Requirement: Tipos de problema identificados por URI estável
Cada categoria de erro da aplicação SHALL usar um `type` fixo no formato `urn:problem-type:<slug>` e um `title` fixo, iguais para todas as ocorrências da mesma categoria: `validation-error`, `malformed-request`, `invalid-parameter`, `unauthorized`, `forbidden`, `resource-not-found`, `business-conflict`, `data-integrity-violation` e `internal-error`.

#### Scenario: JSON malformado
- **WHEN** um cliente autenticado envia `POST /api/categories` com corpo JSON sintaticamente inválido
- **THEN** o sistema responde `400 Bad Request` com `type` "urn:problem-type:malformed-request"

#### Scenario: Parâmetro com tipo inválido
- **WHEN** um cliente autenticado envia `GET /api/products/abc`
- **THEN** o sistema responde `400 Bad Request` com `type` "urn:problem-type:invalid-parameter" e `detail` citando o parâmetro `id`

#### Scenario: Conflito de regra de negócio
- **WHEN** um cliente autenticado cria uma categoria com nome já existente
- **THEN** o sistema responde `409 Conflict` com `type` "urn:problem-type:business-conflict" e `detail` citando o nome duplicado

#### Scenario: Violação de integridade de dados
- **WHEN** uma operação dispara uma violação de restrição do banco de dados
- **THEN** o sistema responde `409 Conflict` com `type` "urn:problem-type:data-integrity-violation" e `detail` genérico, sem a mensagem SQL

### Requirement: Erros padrão do Spring MVC com status correto
Exceções padrão da camada web sem categoria própria da aplicação (por exemplo, método HTTP não suportado, media type não suportado ou não aceitável) SHALL ser respondidas com o status HTTP correspondente em formato Problem Details, com `type` "about:blank" e `title` igual à frase do status, e MUST NOT ser convertidas em `500`.

#### Scenario: Método HTTP não suportado
- **WHEN** um cliente autenticado envia `PATCH /api/categories/1`
- **THEN** o sistema responde `405 Method Not Allowed` com `Content-Type` `application/problem+json`, `type` "about:blank" e `status` 405

#### Scenario: Media type não suportado
- **WHEN** um cliente autenticado envia `POST /api/categories` com `Content-Type: text/plain`
- **THEN** o sistema responde `415 Unsupported Media Type` em formato Problem Details
