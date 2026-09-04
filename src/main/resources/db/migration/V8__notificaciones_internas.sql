-- URBELIX - Notificaciones internas por usuario
CREATE TABLE notificaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    titulo VARCHAR(120) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    enlace VARCHAR(255) NULL,
    creada_en DATETIME(6) NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    usuario_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB;

CREATE INDEX idx_notificaciones_usuario ON notificaciones (usuario_id, leida, creada_en);
