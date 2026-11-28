# Diagrama ER — Eventos e Inscrições

> Referenciado pela [Spec 06 — Modelo de Dados: Eventos](../specs/06-modelo-dados-eventos.md).
> Entidade `users` aparece resumida apenas para mostrar o relacionamento com `events`/
> `event_registrations` — detalhes completos estão na [Spec 03](../specs/03-modelo-dados.md).
> Veja também o [Diagrama UML de classes](./eventos-uml.md), com a visão das entidades Java/JPA.
>
> `city`/`state_uf` são colunas de texto em `events` (sem tabelas `states`/`cities` separadas —
> ver trade-off na Spec 06, seção 5).

```mermaid
erDiagram
    USERS {
        uuid id PK
        varchar email
        varchar role
    }

    EVENTS {
        uuid id PK
        uuid organizer_id FK
        varchar title
        varchar category
        varchar modality
        varchar city
        varchar state_uf
        varchar address
        varchar online_url
        timestamptz starts_at
        timestamptz ends_at
        integer capacity
        varchar status
        timestamptz deleted_at
    }

    EVENT_REGISTRATIONS {
        uuid id PK
        uuid event_id FK
        uuid user_id FK
        varchar status
        timestamptz registered_at
        timestamptz cancelled_at
    }

    USERS ||--o{ EVENTS : "organiza (role PARTNER/ADMIN)"
    EVENTS ||--o{ EVENT_REGISTRATIONS : "recebe"
    USERS ||--o{ EVENT_REGISTRATIONS : "se inscreve"
```

## Leitura rápida das relações

- Um **usuário** com `role` `PARTNER` ou `ADMIN` organiza vários **eventos**.
- Um **evento** recebe várias **inscrições**; um **usuário** pode ter várias inscrições (em eventos
  diferentes), mas no máximo uma por evento (`UNIQUE (event_id, user_id)` em `event_registrations`).
- `city`/`state_uf` ficam direto em `events`, indexados juntos (`idx_events_state_uf_city`) para
  suportar o filtro "listar eventos por estado/cidade" sem precisar de JOIN.
