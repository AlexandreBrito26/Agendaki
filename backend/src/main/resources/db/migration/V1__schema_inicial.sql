-- Modelo de dados inicial (PostgreSQL)
-- Já pensado para multi-tenant: tudo pertence a um negócio (business_id).

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE business (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(80)  NOT NULL UNIQUE,   -- usado na URL pública
    owner_email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    timezone    VARCHAR(50)  NOT NULL DEFAULT 'America/Sao_Paulo',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE service (
    id           BIGSERIAL PRIMARY KEY,
    business_id  BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    duration_min INT NOT NULL CHECK (duration_min > 0),
    price_cents  INT NOT NULL CHECK (price_cents >= 0),
    active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE professional (
    id          BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

-- Quais serviços cada profissional realiza
CREATE TABLE professional_service (
    professional_id BIGINT NOT NULL REFERENCES professional(id) ON DELETE CASCADE,
    service_id      BIGINT NOT NULL REFERENCES service(id) ON DELETE CASCADE,
    PRIMARY KEY (professional_id, service_id)
);

-- Janelas de trabalho semanais (0 = domingo ... 6 = sábado)
CREATE TABLE working_hours (
    id              BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL REFERENCES professional(id) ON DELETE CASCADE,
    weekday         SMALLINT NOT NULL CHECK (weekday BETWEEN 0 AND 6),
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL,
    CHECK (end_time > start_time)
);

CREATE TABLE appointment (
    id              BIGSERIAL PRIMARY KEY,
    business_id     BIGINT NOT NULL REFERENCES business(id) ON DELETE CASCADE,
    professional_id BIGINT NOT NULL REFERENCES professional(id),
    service_id      BIGINT NOT NULL REFERENCES service(id),
    customer_name   VARCHAR(100) NOT NULL,
    customer_phone  VARCHAR(30)  NOT NULL,
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
                    CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    manage_token    VARCHAR(64) NOT NULL UNIQUE,  -- link de cancelar/remarcar
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at),

    -- Garante no banco que o mesmo profissional não tem horários sobrepostos
    EXCLUDE USING gist (
        professional_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status = 'CONFIRMED')
);

CREATE INDEX idx_appointment_prof_start ON appointment (professional_id, starts_at);
CREATE INDEX idx_service_business ON service (business_id);
CREATE INDEX idx_professional_business ON professional (business_id);
