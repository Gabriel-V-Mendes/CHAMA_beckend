# 03 — Mapa do Código (O Que Cada Arquivo Faz)

Este guia resume o papel de cada arquivo do repositório para acelerar o onboarding de novos colaboradores.

---

## 1. Raiz, Build e Configurações

| Arquivo | O que faz |
| :--- | :--- |
| [`pom.xml`](../pom.xml) | Define Java 25, Spring Boot 4.2.0 e dependências (`webmvc`, `validation`, `actuator`, `data-jpa`, `flyway`, `postgresql`, `security`, `oauth2-resource-server`, `h2` para testes). |
| [`ChamaBackendApplication.java`](../src/main/java/com/chama/backend/ChamaBackendApplication.java) | Classe principal `@SpringBootApplication` com `@ConfigurationPropertiesScan` para carregar configurações tipadas. |
| [`application.properties`](../src/main/resources/application.properties) | Configuração base: datasource PostgreSQL via env vars, `ddl-auto=validate`, Flyway, Jackson `SNAKE_CASE` + `fail-on-unknown-properties=true`, parâmetros `app.security.*` e Actuator restrito. |
| [`application-dev.properties`](../src/main/resources/application-dev.properties) | Defaults locais apenas para perfil `dev` (não usar chaves locais em produção). |
| [`application-test.properties`](../src/main/resources/application-test.properties) | Configura banco em memória H2 no modo PostgreSQL + Flyway para testes rápidos e isolados. |
| [`.env.example`](../.env.example) | Modelo seguro das variáveis de ambiente necessárias (nunca commitar `.env`). |
| [`scripts/setup-fedora.sh`](../scripts/setup-fedora.sh) | Script idempotente para instalar JDK 25, Maven e inicializar PostgreSQL 18 com `scram-sha-256` no Fedora. |

---

## 2. Núcleo Transversal (`src/main/java/com/chama/backend/core/`)

### `core/config/`
- [`AppSecurityProperties.java`](../src/main/java/com/chama/backend/core/config/AppSecurityProperties.java) — `record` `@ConfigurationProperties("app.security")` que valida na subida da aplicação o tamanho mínimo da chave JWT (`>= 32` chars), força do BCrypt (`>= 12`), TTLs dos tokens, CORS e limite de rate limit.

### `core/security/`
- [`RequestIdFilter.java`](../src/main/java/com/chama/backend/core/security/RequestIdFilter.java) — Filtro de mais alta precedência (`Ordered.HIGHEST_PRECEDENCE`) que lê/sanitiza `X-Request-Id` ou gera um ID `req_xxxxxxxx`, grava no `MDC` para logs e devolve no header de resposta.
- [`AuthRateLimitFilter.java`](../src/main/java/com/chama/backend/core/security/AuthRateLimitFilter.java) — Filtro aplicado às requisições `POST /api/v1/auth/**` que limita requisições por IP em janelas de 60 segundos, devolvendo `429 RATE_LIMIT_EXCEEDED` ao exceder.
- [`JwtTokenService.java`](../src/main/java/com/chama/backend/core/security/JwtTokenService.java) — Emite Access Tokens JWT assinados com `HS256`, gera Refresh Tokens opacos com `SecureRandom` e calcula seu hash `SHA-256` hexadecimal.
- [`SecurityConfig.java`](../src/main/java/com/chama/backend/core/security/SecurityConfig.java) — Configura `SecurityFilterChain` stateless, rotas públicas vs autenticadas, CORS, headers OWASP, handlers JSON para `401`/`403`, `BCryptPasswordEncoder`, `JwtEncoder`, `JwtDecoder` e conversão da claim `role` para `ROLE_*`.

### `core/exception/`
- [`ApiErrorResponse.java`](../src/main/java/com/chama/backend/core/exception/ApiErrorResponse.java) — Records imutáveis do envelope padrão de erro (`error.code`, `message`, `details`, `request_id`).
- [`AppException.java`](../src/main/java/com/chama/backend/core/exception/AppException.java) — Exceção base de domínio com métodos fábrica (`conflict`, `unauthorized`, `unprocessable`, `notFound`, `tooManyRequests`).
- [`GlobalExceptionHandler.java`](../src/main/java/com/chama/backend/core/exception/GlobalExceptionHandler.java) — `@RestControllerAdvice` que converte erros de validação (`422`), JSON inválido/campos extras (`400`), `AppException`, `AccessDeniedException` (`403`) e exceções inesperadas (`500`) para `ApiErrorResponse` sem expor stack traces.

---

## 3. Módulo de Usuários (`src/main/java/com/chama/backend/modules/user/`)

- [`UserRole.java`](../src/main/java/com/chama/backend/modules/user/model/UserRole.java) — Enum dos papéis (`STUDENT`, `PARTNER`, `ADMIN`) serializado em minúsculas (`student`, `partner`, `admin`).
- [`User.java`](../src/main/java/com/chama/backend/modules/user/model/User.java) — Entidade JPA da tabela `users` (credenciais, papel, flag `is_active`, aceite legal `terms_version` / `terms_accepted_at`, timestamps de auditoria e `deleted_at`).
- [`StudentProfile.java`](../src/main/java/com/chama/backend/modules/user/model/StudentProfile.java) — Entidade JPA da tabela `student_profiles` vinculada 1:1 a `User` (`full_name`, `birth_date`, `city`, `neighborhood`, `guardian_consent_at`).
- [`UserRepository.java`](../src/main/java/com/chama/backend/modules/user/repository/UserRepository.java) e [`StudentProfileRepository.java`](../src/main/java/com/chama/backend/modules/user/repository/StudentProfileRepository.java) — Interfaces `JpaRepository` para consultas de usuários e perfis.

---

## 4. Módulo de Autenticação (`src/main/java/com/chama/backend/modules/auth/`)

- [`RefreshToken.java`](../src/main/java/com/chama/backend/modules/auth/model/RefreshToken.java) — Entidade JPA da tabela `refresh_tokens` armazenando apenas `token_hash`, expiração e timestamp de revogação.
- [`RefreshTokenRepository.java`](../src/main/java/com/chama/backend/modules/auth/repository/RefreshTokenRepository.java) — Busca por `token_hash` e consulta JPQL `@Modifying` para revogar todos os tokens ativos de um usuário.
- [`RegisterRequest.java`](../src/main/java/com/chama/backend/modules/auth/dto/RegisterRequest.java) — DTO de entrada do cadastro com validações estritas e normalização no construtor compacto.
- [`LoginRequest.java`](../src/main/java/com/chama/backend/modules/auth/dto/LoginRequest.java) — DTO de entrada do login com normalização de e-mail (`trim().toLowerCase()`).
- [`RefreshTokenRequest.java`](../src/main/java/com/chama/backend/modules/auth/dto/RefreshTokenRequest.java) — DTO para `/refresh` e `/logout`.
- [`UserSummaryResponse.java`](../src/main/java/com/chama/backend/modules/auth/dto/UserSummaryResponse.java) e [`AuthResponse.java`](../src/main/java/com/chama/backend/modules/auth/dto/AuthResponse.java) — DTOs de saída com tokens e resumo seguro do usuário.
- [`AuthService.java`](../src/main/java/com/chama/backend/modules/auth/service/AuthService.java) — Implementa `register`, `login` (com hash dummy anti-timing), `refresh` (com rotação e detecção de reuso), `logout` e `getCurrentUser`.
- [`AuthController.java`](../src/main/java/com/chama/backend/modules/auth/controller/AuthController.java) — Expõe `/api/v1/auth/register`, `/login`, `/refresh`, `/logout` e `/me`.

---

## 5. Módulo de Saúde (`src/main/java/com/chama/backend/modules/health/`)

- [`HealthController.java`](../src/main/java/com/chama/backend/modules/health/controller/HealthController.java) — Expõe `GET /health/live` (processo ativo), `GET /health/ready` (testa conexão real JDBC com PostgreSQL) e `GET /api/health` (retrocompatibilidade).

---

## 6. Migrações SQL e Testes

- [`V1__init_auth_and_users_schema.sql`](../src/main/resources/db/migration/V1__init_auth_and_users_schema.sql) — Cria tabelas `users`, `student_profiles`, `refresh_tokens`, constraints e índices.
- [`U1__rollback_init_auth_and_users_schema.sql`](../docs/db/rollback/U1__rollback_init_auth_and_users_schema.sql) — Script manual de reversão da `V1`.
- [`ChamaBackendApplicationTests.java`](../src/test/java/com/chama/backend/ChamaBackendApplicationTests.java) — Smoke test de carga completa do contexto Spring + Flyway + JPA + Security.
- [`HealthControllerTest.java`](../src/test/java/com/chama/backend/modules/health/controller/HealthControllerTest.java) — Testa `/health/live`, `/health/ready` e `/api/health`.
- [`AuthServiceTest.java`](../src/test/java/com/chama/backend/modules/auth/service/AuthServiceTest.java) — Testes unitários (Mockito + BCrypt real) de regras LGPD, menor de idade, e-mail duplicado e hash dummy.
- [`AuthIntegrationTest.java`](../src/test/java/com/chama/backend/modules/auth/controller/AuthIntegrationTest.java) — Testes end-to-end HTTP via `MockMvc` cobrindo cadastro, login, `/me`, rotação e reuso de refresh token, logout, Mass Assignment (`400`), validação (`422`), falta de token (`401`) e rate limit (`429`).
