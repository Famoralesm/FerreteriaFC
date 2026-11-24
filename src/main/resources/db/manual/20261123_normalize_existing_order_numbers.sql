-- Normaliza los números de todos los pedidos de testing.
-- Conserva pedidos, productos, clientes, detalles y relaciones; solo cambia order_number.
-- Orden histórico: created_at ascendente y luego id.
-- Ejecutar una sola vez después de desplegar la versión que genera correlativos.

BEGIN;

CREATE SEQUENCE IF NOT EXISTS order_number_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    NO CYCLE;

-- Evita colisiones temporales con los números nuevos ya asignados.
UPDATE orders
SET order_number = 'TMP-' || id::text;

DO $$
DECLARE
    current_order RECORD;
    sequence_value BIGINT := 0;
    group_number BIGINT;
    letter_index INTEGER;
    prefix TEXT;
    consecutive_number INTEGER;
BEGIN
    FOR current_order IN
        SELECT id
        FROM orders
        ORDER BY created_at ASC NULLS FIRST, id ASC
    LOOP
        sequence_value := sequence_value + 1;
        group_number := ((sequence_value - 1) / 9999) + 1;
        consecutive_number := ((sequence_value - 1) % 9999) + 1;
        prefix := '';

        WHILE group_number > 0 LOOP
            group_number := group_number - 1;
            letter_index := (group_number % 26)::INTEGER;
            prefix := chr(65 + letter_index) || prefix;
            group_number := group_number / 26;
        END LOOP;

        UPDATE orders
        SET order_number = prefix || lpad(consecutive_number::TEXT, 4, '0')
        WHERE id = current_order.id;
    END LOOP;

    -- El próximo pedido nuevo recibirá el correlativo inmediatamente siguiente.
    PERFORM setval('order_number_seq', sequence_value + 1, false);
END $$;

COMMIT;
