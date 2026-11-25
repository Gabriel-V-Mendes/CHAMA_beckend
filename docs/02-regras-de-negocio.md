# 02 — Regras de Negócio, Segurança e LGPD

O projeto CHAMA trata dados de jovens e estudantes (incluindo menores de 18 anos). Por isso, regras de domínio, AppSec e conformidade com a **LGPD** caminham juntas no código.

---

## 1. Regras do Cadastro (`POST /api/v1/auth/register`)

1. **Normalização na borda:**
   - `email` tem espaços nas bordas removidos e é convertido para minúsculas (`trim().toLowerCase()`).
   - `full_name`, `city` e `terms_version` sofrem `trim()`; `neighborhood` em branco vira `null`.
2. **Validação estrita de entrada (Bean Validation):**
   - `email`: obrigatório, formato válido, máximo de 254 caracteres.
   - `password`: obrigatória, **8 a 72 caracteres** (limite máximo de 72 bytes do BCrypt para evitar truncamento silencioso ou DoS por senhas gigantes), exigindo pelo menos **1 letra maiúscula, 1 minúscula, 1 número e 1 caractere especial**.
   - `full_name`: obrigatório, 2 a 150 caracteres.
   - `birth_date`: obrigatória e estritamente no passado (`@Past`).
   - `city`: obrigatória, máximo de 100 caracteres; `neighborhood` é opcional.
   - `terms_accepted`: obrigatório e deve ser `true` (`@AssertTrue`).
   - `terms_version`: obrigatória (ex.: `"2026-11"`), registrada com o timestamp `terms_accepted_at` em UTC.
3. **Proteção a Menores de Idade (LGPD Art. 14):**
   - A idade é calculada no servidor comparando `birth_date` com a data atual em **UTC**.
   - Se a idade for **menor que 18 anos**, o campo `guardian_consent` precisa ser explicitamente `true`.
   - Caso contrário, o cadastro é bloqueado com **`422 Unprocessable Content`** e código **`GUARDIAN_CONSENT_REQUIRED`**.
   - Quando consentido, `guardian_consent_at` grava o instante exato do registro.
4. **Unicidade de E-mail:**
   - Se já existir conta com o e-mail normalizado, retorna **`409 Conflict`** (`EMAIL_ALREADY_EXISTS`).
   - Há tratamento adicional de `DataIntegrityViolationException` para proteger contra cadastros simultâneos em condição de corrida.
5. **Prevenção de Mass Assignment / Escalação de Privilégio:**
   - Todo novo cadastro recebe **exclusivamente** o papel `UserRole.STUDENT` (`"student"`) definido no servidor.
   - Qualquer campo extra enviado no JSON (ex.: `"role": "admin"` ou `"is_active": true`) é rejeitado pelo Jackson com **`400 Bad Request`** (`MALFORMED_REQUEST`).
6. **Persistência Atômica e Retorno:**
   - Em uma única transação (`@Transactional`), são criados `users`, `student_profiles` e o primeiro `refresh_tokens`.
   - Retorna **`201 Created`** com `access_token`, `refresh_token`, `token_type: "Bearer"`, `expires_in` e `user` (sem nunca retornar `password_hash`).

---

## 2. Regras do Login (`POST /api/v1/auth/login`)

1. **Higienização de E-mail:** Remove espaços acidentais nas bordas e converte para minúsculas antes de buscar no banco.
2. **Validação de Hash BCrypt:** Compara a senha enviada com `password_hash` armazenado com fator de custo `>= 12`.
3. **Status da Conta:** Usuários inativos (`is_active = false`) ou removidos logicamente (`deleted_at != null`) não podem autenticar.
4. **Proteção contra Enumeração de E-mails (Timing-Safe):**
   - Se o e-mail não existir no banco, o `AuthService` executa `passwordEncoder.matches(...)` contra um hash BCrypt dummy gerado na inicialização da aplicação.
   - Tanto e-mail inexistente quanto senha errada ou conta inativa retornam exatamente **`401 Unauthorized`** com código **`INVALID_CREDENTIALS`** e mensagem genérica (`"E-mail ou senha inválidos."`).

---

## 3. Regras de Sessão, Refresh Token e Logout

### Access Token (JWT)
- Assinado com algoritmo **HMAC-SHA256 (`HS256`)** usando chave simétrica de no mínimo 256 bits (`JWT_SECRET`).
- Expiração curta padrão de **15 minutos (`900` segundos)**.
- Contém as claims `iss` (`chama-backend`), `iat`, `exp`, `sub` (`user.id` em UUID) e `role` (`student` | `partner` | `admin`).

### Refresh Token Opaco e Rotacionável
- Gerado com **32 bytes aleatórios** (`SecureRandom`) codificados em Base64URL.
- **Nunca é salvo em texto puro no banco:** apenas seu hash **SHA-256** (64 caracteres hex) é persistido em `refresh_tokens`, com validade padrão de **7 dias (`604800` segundos)**.
- **Rotação (`POST /api/v1/auth/refresh`):** ao trocar um refresh token válido, o token anterior é marcado como revogado (`revoked_at`) e um novo par `access_token` + `refresh_token` é emitido.
- **Detecção de Reuso (Mitigação de Roubo):** se alguém tentar reutilizar um refresh token já revogado, o sistema interpreta como comprometimento da sessão, **revoga imediatamente todos os refresh tokens ativos daquele usuário** (`noRollbackFor = AppException.class` garante o commit da revogação) e devolve **`401 INVALID_TOKEN`**.
- **Logout (`POST /api/v1/auth/logout`):** revoga o refresh token informado de forma idempotente e retorna **`204 No Content`**.

---

## 4. Proteções Transversais de API

- **Rate Limiting (`AuthRateLimitFilter`):** Limite por IP e rota em janela de 60 segundos para `POST /api/v1/auth/**` (padrão: `10` req/min em produção/dev, `20` req/min em testes). Exceder retorna **`429 Too Many Requests`** com header `Retry-After: 60` e código `RATE_LIMIT_EXCEEDED`.
- **Logs Livres de PII (LGPD):** Logs nunca imprimem e-mail, senha, token, nome completo ou data de nascimento — apenas `user_id` (UUID) e `request_id`.
- **Headers HTTP OWASP:** `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Strict-Transport-Security`, `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'`.
- **CORS Restritivo:** Apenas origens explícitas configuradas em `CORS_ALLOWED_ORIGINS`, sem wildcard `*`.
- **RBAC Previsto:** `student` (estudante), `partner` (escola/parceiro) e `admin` (gestão CHAMA), mapeados para authorities `ROLE_STUDENT`, `ROLE_PARTNER` e `ROLE_ADMIN`.
