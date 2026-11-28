# Spec 06 — Modelo de Dados: Eventos e Inscrições

> Complementa a [Spec 03 — Modelo de Dados](./03-modelo-dados.md). Segue as mesmas convenções
> (PK `UUID`, `created_at`/`updated_at`/`deleted_at`, `snake_case`, tabelas no plural).

## 1. Contexto e Decisões

- Por enquanto só existem eventos do **Rio Grande do Sul**, mas `city`/`state_uf` ficam como colunas
  de texto indexadas em `events` — adicionar eventos de outros estados/cidades é só inserir uma linha
  com valores diferentes, sem migração de schema.
- **Sem tabelas `states`/`cities`:** decisão deliberada para seguir o mesmo padrão já usado em
  `student_profiles.city` (Spec 03), que também é texto livre sem FK. Normalizar em tabelas só traria
  ganho (integridade referencial, catálogo reaproveitável por outras entidades) se outra entidade além
  de `events` também precisar de localização — não é o caso hoje. Ver trade-off detalhado na seção 5.
- **Criação de eventos:** restrita aos papéis `PARTNER` e `ADMIN` (validado na camada de serviço, não
  só no banco — ver regra BOLA/IDOR da [Spec 04](./04-seguranca-lgpd.md)).
- **Inscrição:** automática e imediata (sem aprovação manual, sem lista de espera). Quando `capacity`
  é atingida, novas inscrições são rejeitadas (`409 EVENT_FULL`).
- **Modalidade:** eventos podem ser `PRESENCIAL`, `ONLINE` ou `HIBRIDO`. `city`/`state_uf` só são
  obrigatórios quando há componente físico; link de acesso só é obrigatório quando há componente online.
- Diagrama ER (tabelas): [`docs/diagrams/eventos-er.md`](../diagrams/eventos-er.md).
- Diagrama UML de classes (entidades JPA): [`docs/diagrams/eventos-uml.md`](../diagrams/eventos-uml.md).

## 2. Entidades

### `events` (eventos)
- `id` (UUID, PK)
- `organizer_id` (UUID, FK -> `users.id`, NOT NULL) — quem criou o evento; deve ter `role` `PARTNER`
  ou `ADMIN` (validado no `EventService`, não é um `CHECK` de banco porque `role` vive em `users`)
- `title` (VARCHAR(150), NOT NULL)
- `description` (TEXT, NOT NULL)
- `category` (VARCHAR(30), NOT NULL, CHECK IN `WORKSHOP`, `PALESTRA`, `HACKATHON`, `MENTORIA`,
  `VISITA_TECNICA`, `FEIRA`, `OUTRO`)
- `modality` (VARCHAR(20), NOT NULL, DEFAULT `PRESENCIAL`, CHECK IN `PRESENCIAL`, `ONLINE`, `HIBRIDO`)
- `city` (VARCHAR(100), NULLABLE) — nome livre da cidade, ex.: `Porto Alegre`; obrigatório quando
  `modality` != `ONLINE`
- `state_uf` (VARCHAR(2), NULLABLE) — sigla da UF, ex.: `RS`; obrigatório junto com `city`
- `address` (VARCHAR(255), NULLABLE) — rua/local do evento, texto livre
- `online_url` (VARCHAR(500), NULLABLE) — link de acesso; obrigatório quando `modality` != `PRESENCIAL`
- `starts_at` (TIMESTAMPTZ, NOT NULL)
- `ends_at` (TIMESTAMPTZ, NOT NULL) — `CHECK (ends_at > starts_at)`
- `registration_closes_at` (TIMESTAMPTZ, NULLABLE) — se nulo, inscrições ficam abertas até `starts_at`
- `capacity` (INTEGER, NULLABLE) — `NULL` = sem limite; `CHECK (capacity IS NULL OR capacity > 0)`
- `status` (VARCHAR(20), NOT NULL, DEFAULT `DRAFT`, CHECK IN `DRAFT`, `PUBLISHED`, `CANCELLED`,
  `COMPLETED`) — só eventos `PUBLISHED` aparecem na listagem pública
- `created_at`, `updated_at`, `deleted_at`

Constraints adicionais de consistência de localidade/modalidade:
- `chk_events_location_required_unless_online`: `modality = 'ONLINE' OR (city IS NOT NULL AND state_uf IS NOT NULL)`
- `chk_events_url_required_unless_presencial`: `modality = 'PRESENCIAL' OR online_url IS NOT NULL`
- `chk_events_state_uf_format`: `state_uf IS NULL OR state_uf ~ '^[A-Z]{2}$'` — garante sigla em
  maiúsculas com 2 letras (normalização feita na aplicação antes de persistir)

### `event_registrations` (inscrições em eventos)
- `id` (UUID, PK)
- `event_id` (UUID, FK -> `events.id`, NOT NULL)
- `user_id` (UUID, FK -> `users.id`, NOT NULL)
- `status` (VARCHAR(20), NOT NULL, DEFAULT `CONFIRMED`, CHECK IN `CONFIRMED`, `CANCELLED`)
- `registered_at` (TIMESTAMPTZ, NOT NULL, DEFAULT `CURRENT_TIMESTAMP`)
- `cancelled_at` (TIMESTAMPTZ, NULLABLE)
- `created_at`, `updated_at`
- `UNIQUE (event_id, user_id)` — um usuário tem no máximo uma linha de inscrição por evento;
  cancelar e se inscrever de novo apenas alterna o `status` da mesma linha (não cria linha nova)

## 3. Regras de Negócio Derivadas do Modelo

- **Capacidade (race condition):** a contagem de inscritos (`COUNT(*) WHERE event_id = ? AND status =
  'CONFIRMED'`) e o `INSERT`/`UPDATE` da inscrição devem ocorrer na mesma transação, travando a linha
  do evento (`SELECT ... FOR UPDATE` em `events`) antes de validar `capacity`, para evitar overbooking
  sob concorrência — mesmo padrão de cuidado transacional já usado na rotação de refresh tokens.
- **BOLA/IDOR:** cancelar uma inscrição só pode ser feito pelo próprio `user_id` da inscrição ou por
  `ADMIN`/pelo `organizer_id` do evento. Editar/cancelar um evento só pode ser feito pelo próprio
  `organizer_id` ou por `ADMIN`.
- **Listagem pública:** só retorna eventos com `status = 'PUBLISHED'` e `deleted_at IS NULL`; filtros
  esperados: por `state_uf`, por `city` (exact match ou `ILIKE`), por `category` e por intervalo de
  `starts_at`.
- **Normalização de entrada:** `EventService` deve normalizar `state_uf` para maiúsculas e `city` com
  capitalização consistente antes de salvar, para reduzir variações do mesmo valor (ex.: `"porto
  alegre"` vs `"Porto Alegre"`) — já que não há tabela de referência garantindo isso no banco.
- **Soft delete em `events`:** mantém histórico de eventos passados/cancelados para auditoria e para
  não quebrar o histórico de inscrições já existentes.

## 4. Índices

- `idx_events_state_uf_city` em `events (state_uf, city)` — suporta o filtro combinado
  "estado > cidade" da listagem pública
- `idx_events_organizer_id` em `events (organizer_id)`
- `idx_events_status` em `events (status)`
- `idx_events_starts_at` em `events (starts_at)`
- `idx_event_registrations_event_id` em `event_registrations (event_id)`
- `idx_event_registrations_user_id` em `event_registrations (user_id)`

## 5. Por Que Não Normalizar em `states`/`cities` (Trade-off)

- **A favor de tabelas separadas:** integridade referencial (impede duas grafias da mesma cidade),
  catálogo central reaproveitável por outras entidades (ex.: futura `schools`), espaço para guardar
  código IBGE.
- **A favor de colunas de texto (escolha atual):** consistente com `student_profiles.city` já
  existente no projeto; evita 2 tabelas + JOIN extra em toda consulta de evento; nenhuma outra entidade
  do domínio hoje precisa de localização estruturada.
- **Gatilho para revisitar:** se outra entidade (ex.: `schools`/`partners`) também precisar de
  cidade/estado, ou se surgir um requisito de autocomplete/validação de cidades reais, migrar `city`/
  `state_uf` de `events` para um catálogo normalizado compensa.

## 6. Migração Flyway

Implementada em [`V2__create_events_schema.sql`](../../src/main/resources/db/migration/V2__create_events_schema.sql),
com rollback correspondente em
[`docs/db/rollback/U2__rollback_create_events_schema.sql`](../db/rollback/U2__rollback_create_events_schema.sql).
Cobre apenas `events` — `event_registrations` fica para uma migração `V3` futura, quando a feature de
inscrição for implementada.
