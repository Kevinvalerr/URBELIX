-- URBELIX - Flujo de solicitudes de visitantes gestionado por porteria
ALTER TABLE visitantes
    ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';

ALTER TABLE visitantes
    ADD COLUMN motivo_rechazo VARCHAR(500) NULL;

-- Conserva el significado de los registros historicos creados antes del flujo
-- de solicitudes: una visita sin salida sigue dentro; las demas finalizaron.
UPDATE visitantes
SET estado = CASE
    WHEN fecha_entrada IS NOT NULL AND fecha_salida IS NULL THEN 'DENTRO'
    WHEN fecha_salida IS NOT NULL THEN 'FINALIZADA'
    ELSE 'PENDIENTE'
END;
