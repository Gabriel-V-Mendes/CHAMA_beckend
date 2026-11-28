# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

CHAMA backend: official REST API connecting youth from public schools/underserved communities to tech
opportunities. Java 25 + Spring Boot 4.2.0 + PostgreSQL, modular monolith, with strict OWASP API Top 10
controls and LGPD (Brazilian data-protection law) compliance since the system handles minors' data.

## Commands

```bash
# Run locally (dev profile fills in local defaults for JWT_SECRET/DB_PASSWORD if unset)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Full test suite (uses `test` profile: H2 in-memory in PostgreSQL mode + Flyway, no real DB needed)
./mvnw clean test

# Single test class / single test method
./mvnw test -Dtest=AuthServiceTest
./mvnw test -Dtest=AuthServiceTest#shouldRejectDuplicateEmail

# Healthchecks once running
curl http://localhost:8080/health/live
curl http://localhost:8080/health/ready   # verifies real JDBC connectivity
```

First-time setup: `cp .env.example .env`, then either export those vars or rely on the `dev` profile
defaults. Fedora has `./scripts/setup-fedora.sh` to install JDK 25 + PostgreSQL 18 with `scram-sha-256`.
Full details, Docker alternative, and troubleshooting: `docs/05-ambiente-de-desenvolvimento.md`.

## Architecture

Modular monolith organized by domain under `src/main/java/com/chama/backend/`:

```
core/                      # Cross-cutting infra — not domain-specific
├── config/                # Typed @ConfigurationProperties (e.g. AppSecurityProperties)
├── exception/             # AppException (domain), GlobalExceptionHandler (RFC 7807 envelope)
└── security/              # RequestIdFilter, AuthRateLimitFilter, JwtTokenService, SecurityConfig
modules/<domain>/          # auth, user, health, ...
├── controller/            # HTTP layer, input validation
├── dto/                   # Immutable `record`s for request/response
├── service/               # Business rules, transactions
├── repository/             # Spring Data JPA interfaces
└── model/                 # JPA entities, domain enums
```

New domains (e.g. `opportunities`) follow this same five-folder shape. Check `docs/specs/` before adding
routes or tables — those specs are the source of truth and code should not silently diverge from them.

**Auth/security model** (`core/security/`, `modules/auth/`):
- Stateless JWT (HS256) access tokens, short TTL (~15 min); opaque refresh tokens stored only as
  SHA-256 hashes in `refresh_tokens`, rotated on each use, with reuse detection.
- `JwtTokenService` issues/decodes tokens; `SecurityConfig` wires the stateless filter chain, CORS,
  OWASP security headers, JSON 401/403 handlers, and maps the `role` claim to `ROLE_*`.
- `AuthRateLimitFilter` throttles `POST /api/v1/auth/**` per IP per 60s window (`429` on breach).
- `RequestIdFilter` (highest precedence) sanitizes/generates `X-Request-Id`, puts it in MDC for log
  correlation, and echoes it in the response header and in every error envelope.
- Login uses a dummy hash comparison on the "user not found" path to resist timing attacks.
- Any `/:id` route must verify the resource belongs to `current_user.id` or that the caller is `admin`
  (BOLA/IDOR protection) — this is not automatic, it must be done in the service/repository layer.

**Error handling**: all errors funnel through `GlobalExceptionHandler` into one envelope shape
(`error.code`, `message`, `details[]`, `request_id`) — never leak stack traces or raw DB messages.
`AppException` has factory methods (`conflict`, `unauthorized`, `unprocessable`, `notFound`,
`tooManyRequests`) for raising domain errors from services.

**API conventions** (full contract: `docs/specs/02-contrato-api.md`):
- Base path `/api/v1`; routes are plural, lowercase, kebab-case.
- JSON fields are `snake_case` everywhere; Jackson rejects unknown fields on deserialization
  (`fail-on-unknown-properties=true`) — guards against mass assignment.
- IDs are UUIDv4 (no sequential enumeration). Dates are ISO-8601 UTC.
- List endpoints paginate via `?page=1&page_size=20` (max `page_size=100`), returning `{items, meta}`.
- RBAC roles: `student`, `partner`, `admin`.

**Persistence**: PostgreSQL via Spring Data JPA; `ddl-auto=validate` — schema changes always go through
a new Flyway migration, never through Hibernate auto-DDL. Next migration file goes in
`src/main/resources/db/migration/V{N}__description.sql`, with a corresponding hand-written rollback
script added to `docs/db/rollback/U{N}__rollback_description.sql`, and `docs/specs/03-modelo-dados.md`
updated to match. Tests run the same migrations against H2 in PostgreSQL-compatibility mode, so new SQL
must stay compatible with both.

## Testing conventions

- `*ServiceTest` — unit tests, JUnit 5 + Mockito, no Spring context.
- `*IntegrationTest` — MockMvc + real Spring context + H2 (PostgreSQL mode) + Flyway, exercising full
  HTTP + Security + validation + JPA stack.
- Every fixed bug and every security-critical control (mass assignment, rate limiting, minor consent,
  refresh-token reuse, IDOR) needs a dedicated regression test — see `AuthIntegrationTest` for the
  pattern.

## Non-negotiable constraints

- **LGPD / minors**: collect only what's strictly necessary; never log PII (CPF, email, phone, birth
  date) or `Authorization`/cookies/tokens, even in error paths. Record acceptance of terms (`version` +
  timestamp) rather than inferring it.
- **Secrets**: only via environment variables (see `.env.example`). Never commit real secrets. JWT
  signing key must be ≥32 chars/256 bits; BCrypt cost ≥12.
- **No destructive DB operations** (`DROP`, `TRUNCATE`, unscoped `DELETE`) without explicit confirmation.
- Constructor-based dependency injection only (no field `@Autowired`).
- DTOs are Java `record`s with Jakarta Bean Validation at the boundary.
- Comments in source code and in `docs/` are written in Portuguese (PT-BR); this CLAUDE.md and
  conversational guidance may stay in whatever language the user is using.
- Spec-first: for non-trivial changes, check `docs/specs/*.md` first and call out any drift between
  spec and code before implementing — don't silently implement around a stale spec.

## Where to look for more detail

`docs/README.md` indexes everything; of note:
- `docs/specs/01-arquitetura.md` — architecture ADRs
- `docs/specs/02-contrato-api.md` — full API contract
- `docs/specs/03-modelo-dados.md` — data model
- `docs/specs/04-seguranca-lgpd.md` — AppSec/LGPD requirements
- `docs/specs/05-padroes-codigo.md` — code/git/test conventions
- `docs/03-mapa-do-codigo.md` — per-file breakdown of the entire codebase
