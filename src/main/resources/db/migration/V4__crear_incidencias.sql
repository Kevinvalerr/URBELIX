CREATE TABLE incidencias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    titulo VARCHAR(150) NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,
    categoria VARCHAR(80) NULL,
    prioridad VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    residente_id BIGINT NOT NULL,
    apartamento_id BIGINT NULL,
    motivo_rechazo VARCHAR(1000) NULL,
    observacion_resolucion VARCHAR(1000) NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_incidencia_estado (estado),
    KEY idx_incidencia_prioridad (prioridad),
    KEY idx_incidencia_fecha (fecha_creacion),
    CONSTRAINT fk_incidencia_residente FOREIGN KEY (residente_id) REFERENCES residentes (id),
    CONSTRAINT fk_incidencia_apartamento FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE incidencia_historial (
    id BIGINT NOT NULL AUTO_INCREMENT,
    incidencia_id BIGINT NOT NULL,
    actor_usuario_id BIGINT NULL,
    estado_anterior VARCHAR(20) NULL,
    estado_nuevo VARCHAR(20) NOT NULL,
    comentario VARCHAR(1000) NULL,
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_historial_incidencia (incidencia_id),
    CONSTRAINT fk_historial_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencias (id),
    CONSTRAINT fk_historial_actor FOREIGN KEY (actor_usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
