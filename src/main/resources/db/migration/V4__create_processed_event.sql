-- Avisos CatalogUpdated ya procesados (ADR-0008). La entrega de Kafka es al menos una vez: el
-- eventId identifica un aviso y su clave primaria hace que registrarlo dos veces sea imposible, aun
-- con dos avisos iguales llegando a la vez (ADR-0070).
--
-- Se purga por antigüedad (processed_at) en el chequeo periódico: un aviso viejo que se repitiera
-- después de purgado es inofensivo, porque la decisión sale de comparar versiones con Redis.
--
-- Son datos propios: la base sí valida su contenido.

CREATE TABLE processed_event (
    event_id     TEXT        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_processed_event_processed_at ON processed_event (processed_at);
