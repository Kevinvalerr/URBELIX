ALTER TABLE pagos
    ADD COLUMN resultado_simulacion VARCHAR(30) NULL,
    ADD COLUMN transaccion_simulada VARCHAR(80) NULL,
    ADD COLUMN simulado_en DATETIME(6) NULL;
