-- URBELIX - Migracion MySQL de funcionalidades nuevas
-- Ejecutar una sola vez sobre una base respaldada y con el servicio detenido.
-- Requiere que existan previamente: usuario, apartamentos y residentes.

ALTER TABLE apartamentos
    ADD COLUMN codigo_registro VARCHAR(32) NULL;

ALTER TABLE apartamentos
    ADD CONSTRAINT uk_apartamentos_codigo_registro UNIQUE (codigo_registro);

CREATE TABLE password_reset_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(100) NOT NULL,
    usuario_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_token_token UNIQUE (token),
    CONSTRAINT uk_password_reset_token_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_password_reset_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB;

CREATE TABLE incidencia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asunto VARCHAR(120) NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    respuesta VARCHAR(2000) NULL,
    residente_id BIGINT NOT NULL,
    apartamento_id BIGINT NULL,
    creado_en DATETIME(6) NOT NULL,
    actualizado_en DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_incidencia_residente
        FOREIGN KEY (residente_id) REFERENCES residentes (id),
    CONSTRAINT fk_incidencia_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB;

CREATE TABLE parqueaderos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(255) NOT NULL,
    zona VARCHAR(255) NULL,
    estado VARCHAR(255) NOT NULL,
    apartamento_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_parqueaderos_numero UNIQUE (numero),
    CONSTRAINT fk_parqueaderos_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB;
