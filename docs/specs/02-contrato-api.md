# Spec 02 — Contrato de API (CHAMA_beckend)

## 1. Convenções Gerais
- **Base URL:** `/api/v1`
- **Formato:** `application/json; charset=utf-8`
- **Nomenclatura de rotas:** plural, lowercase e `kebab-case` (ex.: `/api/v1/opportunities`, `/api/v1/school-partners`).
- **Nomenclatura de campos JSON:** `snake_case` em todo o projeto (deserialização estrita rejeitando campos desconhecidos).
- **Datas e Horários:** ISO-8601 em UTC (`2026-11-24T18:30:00Z`).
- **IDs:** UUID v4 (evita enumeração sequencial de recursos).

## 2. Autenticação e Autorização
- Header padrão: `Authorization: Bearer <access_token>`
- Perfis (RBAC) previstos:
  - `student` (jovem/estudante)
  - `partner` (escola/instituição/empresa parceira)
  - `admin` (equipe gestora CHAMA)

### Endpoints de Autenticação (`/api/v1/auth`)
- `POST /api/v1/auth/register` (`201 Created`) — Cadastro de novo estudante + criação do perfil inicial e emissão de tokens.
  - Request: `{ "email", "password", "full_name", "birth_date", "city", "neighborhood?", "terms_accepted": true, "terms_version", "guardian_consent?" }`
  - Response: `{ "access_token", "refresh_token", "token_type": "Bearer", "expires_in", "user": { "id", "email", "full_name", "role", "city" } }`
- `POST /api/v1/auth/login` (`200 OK`) — Autenticação de usuário existente.
  - Request: `{ "email", "password" }`
  - Response: `{ "access_token", "refresh_token", "token_type": "Bearer", "expires_in", "user": { ... } }`
- `POST /api/v1/auth/refresh` (`200 OK`) — Rotaciona o refresh token e emite novo par de tokens.
  - Request: `{ "refresh_token" }`
- `POST /api/v1/auth/logout` (`204 No Content`) — Revoga o refresh token informado.
  - Request: `{ "refresh_token" }`
- `GET /api/v1/auth/me` (`200 OK`, autenticado) — Retorna resumo do usuário logado (`UserSummaryResponse`).

## 3. Paginação Padrão (Listagens)
Query params: `?page=1&page_size=20` (máximo permitido: `page_size=100`).

```json
{
  "items": [],
  "meta": {
    "page": 1,
    "page_size": 20,
    "total_items": 142,
    "total_pages": 8
  }
}
```

## 4. Formato Padrão de Erros (RFC 7807 Simplificado)
Nunca retornar stack traces ou mensagens cruas de banco de dados.

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Os dados enviados são inválidos.",
    "details": [
      { "field": "email", "issue": "Formato de e-mail inválido" }
    ],
    "request_id": "req_9f8e7d6c"
  }
}
```

## 5. Códigos HTTP
- `200 OK` — Leitura ou atualização com sucesso
- `201 Created` — Recurso criado com sucesso
- `204 No Content` — Remoção ou ação sem corpo de resposta
- `400 Bad Request` / `422 Unprocessable Entity` — Erro de validação ou JSON malformado/campos inválidos
- `401 Unauthorized` — Token ausente, inválido, expirado ou credenciais inválidas
- `403 Forbidden` — Autenticado, mas sem permissão para o recurso
- `404 Not Found` — Recurso inexistente (ou ocultado por segurança)
- `409 Conflict` — Violação de unicidade (ex.: e-mail já cadastrado)
- `429 Too Many Requests` — Limite de requisições excedido
- `500 Internal Server Error` — Erro inesperado tratado e logado com `request_id`

## 6. Healthchecks
- `GET /health/live` — Verifica se o processo está de pé.
- `GET /health/ready` — Verifica conectividade com banco de dados e dependências críticas.
- `GET /api/health` — Endpoint legado mantido por retrocompatibilidade.
