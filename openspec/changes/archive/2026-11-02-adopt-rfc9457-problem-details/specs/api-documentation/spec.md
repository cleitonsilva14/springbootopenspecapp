## MODIFIED Requirements

### Requirement: Respostas de erro documentadas
A especificação SHALL documentar, para cada operação, as respostas de erro possíveis (400, 401, 404, 409 conforme aplicável) com o schema `ProblemDetail` (RFC 9457: `type`, `title`, `status`, `detail`, `instance` e a extensão opcional `fieldErrors`) sob o media type `application/problem+json`.

#### Scenario: Erros documentados na criação de produto
- **WHEN** um cliente consulta a operação `POST /api/products` em `/v3/api-docs`
- **THEN** ela lista respostas 201, 400, 401 e 404 com seus esquemas
- **AND** as respostas 400, 401 e 404 referenciam `#/components/schemas/ProblemDetail` sob `application/problem+json`

#### Scenario: Schema ProblemDetail publicado
- **WHEN** um cliente consulta `/v3/api-docs`
- **THEN** `components.schemas.ProblemDetail` define as propriedades `type`, `title`, `status`, `detail`, `instance` e `fieldErrors`
