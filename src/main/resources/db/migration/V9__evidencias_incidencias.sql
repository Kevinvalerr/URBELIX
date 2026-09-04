-- URBELIX - Evidencias privadas asociadas a incidencias/PQRS
CREATE TABLE incidencia_adjunto (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_interno VARCHAR(80) NOT NULL,
    tipo_contenido VARCHAR(100) NOT NULL,
    tamano BIGINT NOT NULL,
    cargado_por VARCHAR(180) NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    incidencia_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_adjunto_nombre_interno (nombre_interno),
    CONSTRAINT fk_adjunto_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencia (id)
) ENGINE=InnoDB;

CREATE INDEX idx_adjunto_incidencia ON incidencia_adjunto (incidencia_id, creado_en);
