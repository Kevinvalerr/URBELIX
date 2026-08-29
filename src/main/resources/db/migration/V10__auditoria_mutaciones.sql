-- URBELIX - Bitacora de mutaciones autorizadas sin datos sensibles
CREATE TABLE auditoria (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_email VARCHAR(180) NOT NULL,
    accion VARCHAR(120) NOT NULL,
    entidad VARCHAR(255) NOT NULL,
    entidad_id BIGINT NULL,
    detalle VARCHAR(500) NULL,
    creado_en DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_auditoria_fecha ON auditoria (creado_en);
CREATE INDEX idx_auditoria_actor ON auditoria (actor_email, creado_en);
