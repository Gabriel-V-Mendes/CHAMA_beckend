# Spec 03 — Modelo de Dados (CHAMA_beckend)

## 1. Convenções de Banco de Dados
- **Chaves Primárias:** `id` do tipo `UUID` (gerado via aplicação ou `gen_random_uuid()`).
- **Auditoria Obrigatória:** Toda tabela de domínio deve conter `created_at` (`TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`) e `updated_at` (`TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`).
- **Soft Delete (quando aplicável):** `deleted_at` (`TIMESTAMPTZ NULL`) para entidades que exigem histórico ou retenção controlada.
- **Nomenclatura:** `snake_case`, tabelas no plural (`users`, `student_profiles`, `refresh_tokens`, `schools`, `opportunities`, `applications`).

## 2. Entidades Principais (Domínio Inicial)

### `users`
Armazena credenciais, papel de acesso e aceite legal (LGPD).
- `id` (UUID, PK)
- `email` (VARCHAR(254), UNIQUE, NOT NULL)
- `password_hash` (TEXT, NOT NULL)
- `role` (VARCHAR(20), CHECK IN `STUDENT`, `PARTNER`, `ADMIN`)
- `is_active` (BOOLEAN, DEFAULT true)
- `terms_version` (VARCHAR(32), NOT NULL) — *versão dos Termos de Uso/Privacidade aceitos*
- `terms_accepted_at` (TIMESTAMPTZ, NOT NULL) — *timestamp do aceite legal*
- `created_at`, `updated_at`, `deleted_at`

### `student_profiles`
Dados do jovem conectado à plataforma (minimização de dados pela LGPD).
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`, UNIQUE)
- `full_name` (VARCHAR(150), NOT NULL)
- `birth_date` (DATE, NOT NULL) — *necessário para verificar regra de menor de idade (LGPD Art. 14)*
- `neighborhood` (VARCHAR(100), NULLABLE)
- `city` (VARCHAR(100), NOT NULL)
- `guardian_consent_at` (TIMESTAMPTZ, NULLABLE) — *registro de consentimento do responsável para menores de 18 anos*
- `created_at`, `updated_at`

### `refresh_tokens`
Armazena exclusivamente o hash SHA-256 dos refresh tokens emitidos para suporte a rotação e revogação.
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`)
- `token_hash` (VARCHAR(64), UNIQUE, NOT NULL)
- `expires_at` (TIMESTAMPTZ, NOT NULL)
- `revoked_at` (TIMESTAMPTZ, NULLABLE)
- `created_at` (TIMESTAMPTZ, NOT NULL)

### Entidades Futuras Planejadas
- `schools` — Escolas públicas e instituições comunitárias parceiras.
- `opportunities` — Cursos, bolsas, mentorias, eventos e vagas de tecnologia.
- `applications` — Inscrições/interesses dos estudantes nas oportunidades.

## 3. Índices e Migrações
- Criar índices explícitos em todas as chaves estrangeiras (`FK`) e colunas usadas em filtros (`role`, `city`, `expires_at`).
- Toda alteração de schema deve ser versionada via **Flyway** em `src/main/resources/db/migration/V{N}__{descricao}.sql`, com script de reversão manual correspondente documentado em `docs/db/rollback/U{N}__rollback_{descricao}.sql`.
