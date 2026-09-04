-- URBELIX - Vehiculos y control de movimientos de parqueaderos
ALTER TABLE parqueaderos
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'CARRO';

CREATE TABLE vehiculos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    placa VARCHAR(10) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    marca VARCHAR(255) NULL,
    modelo VARCHAR(255) NULL,
    color VARCHAR(255) NULL,
    residente_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vehiculos_placa UNIQUE (placa),
    CONSTRAINT fk_vehiculos_residente FOREIGN KEY (residente_id) REFERENCES residentes (id)
) ENGINE=InnoDB;

ALTER TABLE parqueaderos
    ADD COLUMN vehiculo_id BIGINT NULL,
    ADD CONSTRAINT uk_parqueaderos_vehiculo UNIQUE (vehiculo_id),
    ADD CONSTRAINT fk_parqueaderos_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id);

CREATE TABLE movimientos_parqueadero (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vehiculo_id BIGINT NOT NULL,
    parqueadero_id BIGINT NOT NULL,
    fecha_hora_ingreso DATETIME(6) NOT NULL,
    fecha_hora_salida DATETIME(6) NULL,
    estado VARCHAR(20) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_movimientos_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id),
    CONSTRAINT fk_movimientos_parqueadero FOREIGN KEY (parqueadero_id) REFERENCES parqueaderos (id)
) ENGINE=InnoDB;

CREATE INDEX idx_movimientos_estado ON movimientos_parqueadero (estado);
CREATE INDEX idx_movimientos_ingreso ON movimientos_parqueadero (fecha_hora_ingreso);
