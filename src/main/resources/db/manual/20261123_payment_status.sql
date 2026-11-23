-- Ejecutar una sola vez antes de iniciar la versión del backend que incorpora pagos.
-- Los pedidos existentes quedan inicialmente pendientes, sin inventar confirmaciones históricas.

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20);

UPDATE orders
SET payment_status = 'PENDING'
WHERE payment_status IS NULL;

ALTER TABLE orders
    ALTER COLUMN payment_status SET DEFAULT 'PENDING';

ALTER TABLE orders
    ALTER COLUMN payment_status SET NOT NULL;

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_received_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS payment_received_by BIGINT,
    ADD COLUMN IF NOT EXISTS payment_reversed_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS payment_reversed_by BIGINT,
    ADD COLUMN IF NOT EXISTS payment_reversal_reason VARCHAR(100);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_orders_payment_received_by'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_payment_received_by
            FOREIGN KEY (payment_received_by) REFERENCES users(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_orders_payment_reversed_by'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_payment_reversed_by
            FOREIGN KEY (payment_reversed_by) REFERENCES users(id);
    END IF;
END $$;
