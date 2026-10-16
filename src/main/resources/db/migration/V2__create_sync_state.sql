-- Estado de la sincronización del catálogo (ADR-0011): versión local, si hay una sincronización
-- en curso, último éxito y último error.
--
-- Es una sola fila. La crea esta migración y la base no admite otra (id = 1), así que todo
-- cambio es un UPDATE sobre ella, y la condición sobre la versión del ADR-0007 entra en esa
-- misma sentencia.
--
-- A diferencia de las tablas del catálogo, estos datos son propios: la base sí valida su
-- contenido.

CREATE TABLE sync_state (
    id                INTEGER     PRIMARY KEY,
    -- Última versión aplicada por completo. NULL: todavía no hay copia local.
    local_version     BIGINT,
    sync_in_progress  BOOLEAN     NOT NULL,
    last_success_at   TIMESTAMPTZ,
    last_success_type TEXT,
    last_error_at     TIMESTAMPTZ,
    last_error        TEXT,
    CONSTRAINT ck_sync_state_single_row CHECK (id = 1),
    CONSTRAINT ck_sync_state_last_success_type CHECK (last_success_type IN ('SNAPSHOT', 'INCREMENTAL'))
);

INSERT INTO sync_state (id, local_version, sync_in_progress) VALUES (1, NULL, FALSE);
