# 04 — Fluxos do Sistema

Este documento descreve como os principais fluxos da aplicação operam ponta a ponta.

---

## 1. Fluxo de Cadastro (`POST /api/v1/auth/register`)

```mermaid
sequenceDiagram
    actor U as "Estudante / Frontend"
    participant F as "Filtros (RequestId + RateLimit)"
    participant C as "AuthController"
    participant S as "AuthService"
    participant DB as "PostgreSQL"

    U->>F: "POST /api/v1/auth/register (JSON snake_case)"
    F->>C: "Valida rate limit (IP) e deserializa DTO estrito"
    alt "Campo desconhecido ou JSON inválido"
        C-->>U: "400 MALFORMED_REQUEST"
    else "Falha de Bean Validation (senha fraca, termos false, etc.)"
        C-->>U: "422 VALIDATION_ERROR (details em snake_case)"
    else "Entrada válida"
        C->>S: "register(RegisterRequest)"
        S->>S: "Calcula idade em UTC"
        alt "Menor de 18 anos sem guardian_consent=true"
            S-->>U: "422 GUARDIAN_CONSENT_REQUIRED"
        else "E-mail já cadastrado"
            S->>DB: "existsByEmailIgnoreCase(email)"
            DB-->>S: "true"
            S-->>U: "409 EMAIL_ALREADY_EXISTS"
        else "Dados elegíveis"
            S->>DB: "INSERT users (role=STUDENT, BCrypt cost 12)"
            S->>DB: "INSERT student_profiles"
            S->>DB: "INSERT refresh_tokens (hash SHA-256)"
            S-->>U: "201 Created (access_token, refresh_token, user)"
        end
    end
```

---

## 2. Fluxo de Login (`POST /api/v1/auth/login`)

```mermaid
sequenceDiagram
    actor U as "Usuário / Frontend"
    participant C as "AuthController"
    participant S as "AuthService"
    participant DB as "PostgreSQL"

    U->>C: "POST /api/v1/auth/login (email, password)"
    Note over C: "email normalizado: trim().toLowerCase()"
    C->>S: "login(LoginRequest)"
    S->>DB: "findByEmailIgnoreCase(email)"
    alt "Usuário não encontrado"
        S->>S: "Compara senha com dummyBcryptHash (anti-timing)"
        S-->>U: "401 INVALID_CREDENTIALS"
    else "Senha incorreta ou conta inativa/excluída"
        S->>S: "passwordEncoder.matches(raw, hash)"
        S-->>U: "401 INVALID_CREDENTIALS"
    else "Credenciais válidas"
        S->>DB: "Busca student_profiles e salva novo refresh_token (SHA-256)"
        S-->>U: "200 OK (access_token, refresh_token, user)"
    end
```

---

## 3. Fluxo de Acesso Autenticado (`GET /api/v1/auth/me`)

1. O cliente envia `Authorization: Bearer <access_token>`.
2. O `BearerTokenAuthenticationFilter` usa o `JwtDecoder` (`HS256`) para validar assinatura, expiração (`exp`) e emissor (`iss`).
3. O `JwtAuthenticationConverter` extrai a claim `role` e gera a autoridade Spring Security (`ROLE_STUDENT`, `ROLE_PARTNER` ou `ROLE_ADMIN`).
4. O `AuthController.me(@AuthenticationPrincipal Jwt jwt)` extrai o `sub` (UUID do usuário) e chama `AuthService.getCurrentUser(userId)`.
5. Se o token estiver ausente, adulterado ou expirado, o `AuthenticationEntryPoint` responde `401 UNAUTHORIZED` no envelope padrão `ApiErrorResponse`.

---

## 4. Fluxo de Rotação de Refresh Token, Detecção de Reuso e Logout

```mermaid
sequenceDiagram
    actor U as "Cliente"
    participant S as "AuthService"
    participant DB as "PostgreSQL (refresh_tokens)"

    U->>S: "POST /api/v1/auth/refresh (refresh_token_1)"
    S->>DB: "findByTokenHash(sha256(refresh_token_1))"
    alt "Token já estava revogado (reuso detectado)"
        S->>DB: "UPDATE refresh_tokens SET revoked_at = now WHERE user_id = ? (commit garantido)"
        S-->>U: "401 INVALID_TOKEN"
    else "Token expirado ou conta inativa"
        S-->>U: "401 INVALID_TOKEN"
    else "Token válido"
        S->>DB: "Revoga refresh_token_1 e grava hash de refresh_token_2"
        S-->>U: "200 OK (novo access_token + refresh_token_2)"
    end
```

- **Logout (`POST /api/v1/auth/logout`):** calcula o hash `SHA-256` do `refresh_token` enviado, marca `revoked_at` se ainda ativo e devolve `204 No Content`.

---

## 5. Fluxo de Erros Padronizados e Rate Limit

- Toda exceção capturada pelo `GlobalExceptionHandler` ou pelos handlers do `SecurityConfig` devolve o mesmo formato JSON:
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
- Quando um IP ultrapassa o limite em `POST /api/v1/auth/**`, o `AuthRateLimitFilter` interrompe a requisição antes de chegar ao controller/banco, retornando `429 Too Many Requests`, header `Retry-After: 60` e código `RATE_LIMIT_EXCEEDED`.
