-- Schema de eventos (Spec 06). Ainda sem event_registrations (feature futura).

CREATE TABLE events (
    id UUID PRIMARY KEY,
    organizer_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(30) NOT NULL,
    modality VARCHAR(20) NOT NULL DEFAULT 'PRESENCIAL',
    city VARCHAR(100) NULL,
    state_uf VARCHAR(2) NULL,
    address VARCHAR(255) NULL,
    online_url VARCHAR(500) NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    registration_closes_at TIMESTAMP WITH TIME ZONE NULL,
    capacity INTEGER NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users (id),
    CONSTRAINT chk_events_category CHECK (category IN ('WORKSHOP', 'PALESTRA', 'HACKATHON', 'MENTORIA', 'VISITA_TECNICA', 'FEIRA', 'OUTRO')),
    CONSTRAINT chk_events_modality CHECK (modality IN ('PRESENCIAL', 'ONLINE', 'HIBRIDO')),
    CONSTRAINT chk_events_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED')),
    CONSTRAINT chk_events_dates CHECK (ends_at > starts_at),
    CONSTRAINT chk_events_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT chk_events_location_required_unless_online CHECK (modality = 'ONLINE' OR (city IS NOT NULL AND state_uf IS NOT NULL)),
    CONSTRAINT chk_events_url_required_unless_presencial CHECK (modality = 'PRESENCIAL' OR online_url IS NOT NULL)
);

-- Formato da sigla da UF (2 letras maiúsculas) é validado na aplicação (Bean Validation),
-- não via regex no CHECK, para manter a migração portável entre PostgreSQL e H2 (testes).

CREATE INDEX idx_events_state_uf_city ON events (state_uf, city);
CREATE INDEX idx_events_organizer_id ON events (organizer_id);
CREATE INDEX idx_events_status ON events (status);
CREATE INDEX idx_events_starts_at ON events (starts_at);
