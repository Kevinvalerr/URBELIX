-- URBELIX - Desactivacion segura de cuentas
ALTER TABLE usuario
    ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
