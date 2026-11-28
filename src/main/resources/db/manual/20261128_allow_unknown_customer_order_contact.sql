-- Permite registrar pedidos de clientes desconocidos sin datos de despacho.
ALTER TABLE orders ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN address DROP NOT NULL;
