# Spec Delta

## Purpose

Autentica clientes da API trocando credenciais por um token JWT de curta duração e exige esse token, de forma stateless, para acessar os recursos protegidos do catálogo.

## ADDED Requirements

### Requirement: Login emite token JWT
O sistema SHALL expor `POST /api/auth/login`, acessível sem autenticação, que recebe `username` e `password` obrigatórios e, para credenciais válidas, responde `200 OK` com `accessToken`, `tokenType` igual a `Bearer` e `expiresIn` (segundos).

#### Scenario: Credenciais válidas
- **GIVEN** existe o usuário configurado `admin` com sua senha
- **WHEN** o cliente envia `POST /api/auth/login` com as credenciais corretas
- **THEN** o sistema responde `200 OK` com `accessToken` não vazio, `tokenType` "Bearer" e `expiresIn` positivo

#### Scenario: Credenciais inválidas
- **WHEN** o cliente envia `POST /api/auth/login` com senha incorreta ou usuário inexistente
- **THEN** o sistema responde `401 Unauthorized` com mensagem genérica que não revela qual dado está incorreto

#### Scenario: Corpo de login inválido
- **WHEN** o cliente envia `POST /api/auth/login` sem `username` ou `password`
- **THEN** o sistema responde `400 Bad Request` com os erros de validação

### Requirement: Conteúdo e validade do token
O token emitido MUST ser assinado com segredo do servidor (HMAC-SHA256), conter o usuário como `subject`, `issuedAt` e `expiration`, e MUST expirar após o tempo configurado (padrão 1 hora).

#### Scenario: Token expirado
- **GIVEN** um token cuja expiração já passou
- **WHEN** o cliente o utiliza para acessar `GET /api/products`
- **THEN** o sistema responde `401 Unauthorized`

### Requirement: Endpoints do catálogo exigem JWT válido
O sistema MUST exigir o cabeçalho `Authorization: Bearer <token>` com JWT válido para qualquer requisição a `/api/products/**` e `/api/categories/**`, respondendo `401 Unauthorized` em formato JSON padronizado quando ausente, malformado, com assinatura inválida ou expirado.

#### Scenario: Requisição sem token
- **WHEN** o cliente envia `GET /api/categories` sem cabeçalho `Authorization`
- **THEN** o sistema responde `401 Unauthorized` com corpo JSON de erro

#### Scenario: Token com assinatura inválida
- **WHEN** o cliente envia `GET /api/products` com um token adulterado ou assinado com outro segredo
- **THEN** o sistema responde `401 Unauthorized`

#### Scenario: Token válido
- **GIVEN** um token obtido em `/api/auth/login` dentro do prazo de validade
- **WHEN** o cliente envia `GET /api/products` com `Authorization: Bearer <token>`
- **THEN** a requisição é processada e o sistema responde `200 OK`

### Requirement: Sessão stateless
O sistema MUST NOT criar sessão HTTP nem emitir cookie de sessão; cada requisição protegida SHALL ser autenticada exclusivamente pelo token enviado nela.

#### Scenario: Ausência de cookie de sessão
- **WHEN** o cliente realiza login e acessa um endpoint protegido com o token
- **THEN** nenhuma resposta contém o cabeçalho `Set-Cookie` com identificador de sessão

### Requirement: Endpoints públicos
O sistema SHALL permitir acesso sem autenticação a `/api/auth/**`, à Swagger UI (`/swagger-ui/**`, `/swagger-ui.html`) e à especificação OpenAPI (`/v3/api-docs/**`).

#### Scenario: Acesso anônimo à documentação
- **WHEN** um cliente sem token acessa `/v3/api-docs`
- **THEN** o sistema responde `200 OK`

### Requirement: Credenciais de usuário protegidas
As senhas dos usuários MUST ser armazenadas apenas na forma de hash (BCrypt) e o segredo de assinatura e as credenciais iniciais MUST ser configuráveis por propriedades ou variáveis de ambiente, sem exigir alteração de código.

#### Scenario: Configuração por variável de ambiente
- **GIVEN** a aplicação é iniciada com segredo JWT e senha do usuário definidos por variáveis de ambiente
- **WHEN** o cliente realiza login com a senha definida
- **THEN** o sistema emite um token assinado com o segredo configurado
