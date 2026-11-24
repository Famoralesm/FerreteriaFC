-- Unidad base por producto y reglas decimales de venta, expresadas en esa unidad.
-- El campo products.unit se conserva como legado para compatibilidad histórica.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS base_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS minimum_sale_quantity NUMERIC(14, 4),
    ADD COLUMN IF NOT EXISTS sale_quantity_increment NUMERIC(14, 4);

UPDATE products
SET minimum_sale_quantity = 1
WHERE minimum_sale_quantity IS NULL;

UPDATE products
SET sale_quantity_increment = 1
WHERE sale_quantity_increment IS NULL;

ALTER TABLE products
    ALTER COLUMN minimum_sale_quantity SET DEFAULT 1,
    ALTER COLUMN minimum_sale_quantity SET NOT NULL,
    ALTER COLUMN sale_quantity_increment SET DEFAULT 1,
    ALTER COLUMN sale_quantity_increment SET NOT NULL;

ALTER TABLE products
    ALTER COLUMN wholesale_min_quantity TYPE NUMERIC(14, 4)
    USING wholesale_min_quantity::NUMERIC(14, 4);

CREATE INDEX IF NOT EXISTS idx_products_base_unit
    ON products(base_unit_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_products_base_unit'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_base_unit
            FOREIGN KEY (base_unit_id) REFERENCES units_of_measure(id)
            ON DELETE RESTRICT;
    END IF;
END $$;
