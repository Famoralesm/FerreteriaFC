-- Permite registrar pedidos de clientes desconocidos sin datos de despacho.
ALTER TABLE orders ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE orders ALTER COLUMN address DROP NOT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS city varchar(150);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS commune varchar(150);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_rut varchar(12);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_business_name varchar(200);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_giro varchar(150);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_email varchar(150);
