-- URBELIX - Cambio obligatorio de contraseña en el primer ingreso
ALTER TABLE usuario
    ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;
