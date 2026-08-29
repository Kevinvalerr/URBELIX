-- Fecha efectiva en que una obligación fue confirmada.
ALTER TABLE pagos
    ADD COLUMN fecha_pago DATE NULL;

-- Conserva una fecha útil para pagos históricos que ya estaban marcados como pagados.
UPDATE pagos
SET fecha_pago = fecha
WHERE estado_pago = 'PAGADO'
  AND fecha_pago IS NULL;
