-- Ferretería FC: estructura central de auditoría.
-- Ejecutar una sola vez antes de iniciar la versión que incorpora audit_logs.
-- Conserva la tabla audit_log antigua y sus registros, si existe.

DO $$
BEGIN
    IF to_regclass('public.audit_logs') IS NULL
       AND to_regclass('public.audit_log') IS NOT NULL THEN
        ALTER TABLE audit_log RENAME TO audit_logs;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_uuid UUID,
    user_id BIGINT,
    user_name_snapshot VARCHAR(150),
    store_id BIGINT,
    store_name_snapshot VARCHAR(100),
    module VARCHAR(40),
    entity_type VARCHAR(60),
    entity_id VARCHAR(100),
    action VARCHAR(40),
    old_values JSONB,
    new_values JSONB,
    description TEXT,
    origin VARCHAR(30),
    created_at TIMESTAMP WITHOUT TIME ZONE
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'audit_logs' AND column_name = 'entity'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'audit_logs' AND column_name = 'entity_type'
    ) THEN
        ALTER TABLE audit_logs RENAME COLUMN entity TO entity_type;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'audit_logs' AND column_name = 'performed_by'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'audit_logs' AND column_name = 'user_name_snapshot'
    ) THEN
        ALTER TABLE audit_logs RENAME COLUMN performed_by TO user_name_snapshot;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'audit_logs' AND column_name = 'performed_at'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'audit_logs' AND column_name = 'created_at'
    ) THEN
        ALTER TABLE audit_logs RENAME COLUMN performed_at TO created_at;
    END IF;
END $$;

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS event_uuid UUID,
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS user_name_snapshot VARCHAR(150),
    ADD COLUMN IF NOT EXISTS store_id BIGINT,
    ADD COLUMN IF NOT EXISTS store_name_snapshot VARCHAR(100),
    ADD COLUMN IF NOT EXISTS module VARCHAR(40),
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(60),
    ADD COLUMN IF NOT EXISTS entity_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS action VARCHAR(40),
    ADD COLUMN IF NOT EXISTS old_values JSONB,
    ADD COLUMN IF NOT EXISTS new_values JSONB,
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS origin VARCHAR(30),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITHOUT TIME ZONE;

-- Convierte el identificador histórico numérico a texto para admitir IDs de distintas entidades.
ALTER TABLE audit_logs
    ALTER COLUMN entity_id TYPE VARCHAR(100) USING entity_id::text;

UPDATE audit_logs
SET event_uuid = gen_random_uuid()
WHERE event_uuid IS NULL;

UPDATE audit_logs
SET module = COALESCE(module, 'LEGACY'),
    entity_type = COALESCE(entity_type, 'UNKNOWN'),
    action = COALESCE(action, 'LEGACY'),
    origin = COALESCE(origin, 'LEGACY'),
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP);

ALTER TABLE audit_logs
    ALTER COLUMN event_uuid SET DEFAULT gen_random_uuid(),
    ALTER COLUMN event_uuid SET NOT NULL,
    ALTER COLUMN module SET NOT NULL,
    ALTER COLUMN entity_type SET NOT NULL,
    ALTER COLUMN action SET NOT NULL,
    ALTER COLUMN origin SET NOT NULL,
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN created_at SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_audit_logs_user') THEN
        ALTER TABLE audit_logs
            ADD CONSTRAINT fk_audit_logs_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_audit_logs_store') THEN
        ALTER TABLE audit_logs
            ADD CONSTRAINT fk_audit_logs_store
            FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_logs_event_uuid ON audit_logs(event_uuid);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_module_entity ON audit_logs(module, entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_store ON audit_logs(store_id);

-- La aplicación no tendrá endpoints ni métodos de repositorio para editar o borrar eventos.
-- Este trigger añade protección a nivel de base de datos para el rol de aplicación.
CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Los registros de auditoría son inmutables';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_logs_immutable ON audit_logs;
CREATE TRIGGER trg_audit_logs_immutable
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_mutation();
