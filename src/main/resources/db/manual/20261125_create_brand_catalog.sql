CREATE TABLE IF NOT EXISTS brands (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS brand_id BIGINT;

INSERT INTO brands (name, active)
SELECT MIN(TRIM(p.brand)), TRUE
FROM products p
WHERE p.brand IS NOT NULL AND TRIM(p.brand) <> ''
GROUP BY LOWER(TRIM(p.brand))
ON CONFLICT (name) DO NOTHING;

UPDATE products p
SET brand_id = b.id
FROM brands b
WHERE p.brand_id IS NULL
  AND p.brand IS NOT NULL
  AND LOWER(TRIM(p.brand)) = LOWER(b.name);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_products_brand'
          AND table_name = 'products'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_brand
            FOREIGN KEY (brand_id) REFERENCES brands(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_products_brand_id ON products(brand_id);
