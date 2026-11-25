-- Hito 4: registrar cada oferta por producto, proveedor, marca y presentación.
-- Ejecutar una sola vez después de actualizar el backend.

ALTER TABLE product_supplier
    DROP CONSTRAINT IF EXISTS uk_product_supplier;

ALTER TABLE product_supplier
    ADD COLUMN IF NOT EXISTS brand_id BIGINT,
    ADD COLUMN IF NOT EXISTS product_unit_id BIGINT,
    ADD COLUMN IF NOT EXISTS conversion_factor_snapshot NUMERIC(14,4),
    ADD COLUMN IF NOT EXISTS unit_code_snapshot VARCHAR(30),
    ADD COLUMN IF NOT EXISTS unit_name_snapshot VARCHAR(80);

-- Los precios antiguos se interpretan como precios por unidad base.
-- Si existe la presentación que representa la unidad base, se vincula y se
-- conserva su factor. Los registros sin unidad base mantienen factor 1.
UPDATE product_supplier ps
SET brand_id = p.brand_id,
    product_unit_id = pu.id,
    conversion_factor_snapshot = COALESCE(pu.conversion_factor, 1),
    unit_code_snapshot = COALESCE(pu.code, uom.code, 'BASE'),
    unit_name_snapshot = COALESCE(pu.name, uom.name, p.unit, 'Unidad base')
FROM products p
LEFT JOIN units_of_measure uom ON uom.id = p.base_unit_id
LEFT JOIN product_units pu ON pu.product_id = p.id AND pu.code = uom.code
WHERE ps.product_id = p.id
  AND (ps.conversion_factor_snapshot IS NULL OR ps.unit_name_snapshot IS NULL);

UPDATE product_supplier
SET conversion_factor_snapshot = 1
WHERE conversion_factor_snapshot IS NULL OR conversion_factor_snapshot <= 0;

ALTER TABLE product_supplier
    ALTER COLUMN conversion_factor_snapshot SET DEFAULT 1,
    ALTER COLUMN conversion_factor_snapshot SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_product_supplier_brand') THEN
        ALTER TABLE product_supplier
            ADD CONSTRAINT fk_product_supplier_brand
            FOREIGN KEY (brand_id) REFERENCES brands(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_product_supplier_unit') THEN
        ALTER TABLE product_supplier
            ADD CONSTRAINT fk_product_supplier_unit
            FOREIGN KEY (product_unit_id) REFERENCES product_units(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_product_supplier_offer') THEN
        ALTER TABLE product_supplier
            ADD CONSTRAINT uk_product_supplier_offer
            UNIQUE (product_id, supplier_id, brand_id, product_unit_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_product_supplier_product_price
    ON product_supplier(product_id, purchase_price);
