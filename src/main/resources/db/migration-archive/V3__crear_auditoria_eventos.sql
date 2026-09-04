CREATE TABLE auditoria_eventos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_usuario_id BIGINT NULL,
    fecha_hora DATETIME(6) NOT NULL,
    accion VARCHAR(80) NOT NULL,
    modulo VARCHAR(80) NOT NULL,
    entidad VARCHAR(80) NULL,
    entidad_id BIGINT NULL,
    resultado VARCHAR(20) NOT NULL,
    descripcion VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    KEY idx_auditoria_fecha (fecha_hora),
    KEY idx_auditoria_actor (actor_usuario_id),
    CONSTRAINT fk_auditoria_actor FOREIGN KEY (actor_usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
