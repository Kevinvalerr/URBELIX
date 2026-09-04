-- Migracion no destructiva: conserva las columnas legacy para compatibilidad.
ALTER TABLE residentes
    ADD COLUMN correo VARCHAR(255) NULL;

ALTER TABLE usuario
    ADD COLUMN debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE residente_apartamento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    residente_id BIGINT NOT NULL,
    apartamento_id BIGINT NOT NULL,
    fecha_asignacion DATE NULL,
    fecha_fin DATE NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_residente_apartamento_residente
        FOREIGN KEY (residente_id) REFERENCES residentes (id),
    CONSTRAINT fk_residente_apartamento_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id),
    CONSTRAINT uk_residente_apartamento_activo
        UNIQUE (residente_id, apartamento_id, activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

INSERT INTO residente_apartamento
    (residente_id, apartamento_id, fecha_asignacion, activo)
SELECT r.id, r.apartamento_id, NULL, TRUE
FROM residentes r
WHERE r.apartamento_id IS NOT NULL;