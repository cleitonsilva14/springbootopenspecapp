# Spec Delta

## Purpose

Publica a especificação OpenAPI da API e uma interface Swagger UI interativa, permitindo que consumidores descubram os endpoints e os testem com autenticação Bearer JWT.

## ADDED Requirements

### Requirement: Especificação OpenAPI publicada
O sistema SHALL publicar a especificação OpenAPI 3 em `/v3/api-docs`, contendo título, versão e descrição da API e todos os endpoints de autenticação, categorias e produtos com seus esquemas de requisição e resposta.

#### Scenario: Consulta da especificação
- **WHEN** um cliente acessa `GET /v3/api-docs`
- **THEN** o sistema responde `200 OK` com um documento OpenAPI que inclui os caminhos `/api/auth/login`, `/api/categories` e `/api/products`

### Requirement: Swagger UI disponível
O sistema SHALL servir a Swagger UI em `/swagger-ui/index.html`, acessível sem autenticação.

#### Scenario: Acesso à Swagger UI
- **WHEN** um cliente sem token acessa `/swagger-ui/index.html`
- **THEN** o sistema responde `200 OK` com a interface da Swagger UI

### Requirement: Esquema de segurança Bearer documentado
A especificação MUST declarar um esquema de segurança HTTP `bearer` com formato `JWT` e aplicá-lo aos endpoints de categorias e produtos, permitindo informar o token pelo botão "Authorize" da Swagger UI; o endpoint de login MUST aparecer sem exigência de segurança.

#### Scenario: Endpoints protegidos marcados na especificação
- **WHEN** um cliente consulta `/v3/api-docs`
- **THEN** `components.securitySchemes` contém um esquema `bearer` com `bearerFormat` "JWT"
- **AND** as operações de `/api/products` e `/api/categories` referenciam esse esquema
- **AND** a operação `POST /api/auth/login` não exige segurança

### Requirement: Respostas de erro documentadas
A especificação SHALL documentar, para cada operação, as respostas de erro possíveis (400, 401, 404, 409 conforme aplicável) com o esquema de erro padronizado.

#### Scenario: Erros documentados na criação de produto
- **WHEN** um cliente consulta a operação `POST /api/products` em `/v3/api-docs`
- **THEN** ela lista respostas 201, 400, 401 e 404 com seus esquemas
