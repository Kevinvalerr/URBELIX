-- URBELIX - Avisos generales para la comunidad
CREATE TABLE avisos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    titulo VARCHAR(120) NOT NULL,
    contenido VARCHAR(2000) NOT NULL,
    publicado_en DATETIME(6) NOT NULL,
    vence_en DATETIME(6) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_avisos_visibles ON avisos (activo, vence_en, publicado_en);
