-- Reglas de venta por presentación comercial.
-- Los valores se expresan en la propia presentación, no en la unidad base.
-- Se mantienen NULL para filas antiguas: el backend aplica compatibilidad
-- (reglas antiguas del producto para la presentación base y 1/1 para las demás).

ALTER TABLE product_units
    ADD COLUMN IF NOT EXISTS minimum_sale_quantity NUMERIC(14, 4),
    ADD COLUMN IF NOT EXISTS sale_quantity_increment NUMERIC(14, 4);
