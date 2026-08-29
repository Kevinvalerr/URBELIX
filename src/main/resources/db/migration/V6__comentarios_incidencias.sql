-- URBELIX - Conversacion y trazabilidad de incidencias
CREATE TABLE incidencia_comentario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contenido VARCHAR(2000) NOT NULL,
    autor_nombre VARCHAR(160) NOT NULL,
    autor_email VARCHAR(180) NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    incidencia_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comentario_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencia (id)
) ENGINE=InnoDB;

CREATE INDEX idx_comentario_incidencia ON incidencia_comentario (incidencia_id, creado_en);
