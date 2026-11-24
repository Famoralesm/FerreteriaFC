-- Correlativo global de pedidos.
-- Los pedidos antiguos conservan su UUID; los nuevos usarán A0001, A0002... Z9999, AA0001...
-- Ejecutar una sola vez antes de crear pedidos con la nueva versión.

CREATE SEQUENCE IF NOT EXISTS order_number_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    NO CYCLE;
