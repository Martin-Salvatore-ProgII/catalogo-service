-- Copia local del catálogo que publica la cátedra (REF §7, §14.4). Las tres tablas tienen los
-- mismos campos que sus entidades.
--
-- El id de cada fila es el que asigna la cátedra: la base no genera ids. Así, aplicar dos veces
-- el mismo cambio escribe sobre la misma fila y el resultado no cambia.
--
-- Los textos no tienen largo máximo y casi no hay restricciones de contenido: son datos de la
-- cátedra, y un valor inesperado no tiene que frenar la sincronización (RNF-09). La base
-- garantiza la estructura y las relaciones, no reglas de negocio ajenas.

CREATE TABLE professional_category (
    id          BIGINT      PRIMARY KEY,
    name        TEXT        NOT NULL,
    description TEXT,
    -- No hay borrado: una baja de la cátedra llega como enabled = false.
    enabled     BOOLEAN     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE professional (
    id          BIGINT      PRIMARY KEY,
    category_id BIGINT      NOT NULL REFERENCES professional_category (id),
    first_name  TEXT        NOT NULL,
    last_name   TEXT        NOT NULL,
    enabled     BOOLEAN     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE weekly_schedule (
    id                    BIGINT      PRIMARY KEY,
    professional_id       BIGINT      NOT NULL REFERENCES professional (id),
    day_of_week           TEXT        NOT NULL,
    -- Horas locales de atención, sin zona, tal como las publica la cátedra (REF §4).
    start_time            TIME        NOT NULL,
    end_time              TIME        NOT NULL,
    slot_duration_minutes INTEGER     NOT NULL,
    enabled               BOOLEAN     NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,
    -- La única restricción de contenido: la lista de días la fija el contrato (REF §7).
    CONSTRAINT ck_weekly_schedule_day_of_week CHECK (day_of_week IN
        ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
);

-- Casi todas las consultas recorren estas relaciones: profesionales de una categoría y
-- horarios de un profesional.
CREATE INDEX ix_professional_category_id ON professional (category_id);
CREATE INDEX ix_weekly_schedule_professional_id ON weekly_schedule (professional_id);
