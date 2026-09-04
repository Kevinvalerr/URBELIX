-- URBELIX - Esquema base para instalaciones nuevas.
-- Las migraciones V1-V12 evolucionan estas tablas en orden.

CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN', 'PORTERIA', 'RESIDENTE') NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_email (email)
) ENGINE=InnoDB;

CREATE TABLE apartamentos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(255) NULL,
    torre VARCHAR(255) NULL,
    piso INT NULL,
    estado VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE residentes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NULL,
    documento VARCHAR(255) NOT NULL,
    telefono VARCHAR(255) NOT NULL,
    apartamento_id BIGINT NULL,
    usuario_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_residentes_usuario (usuario_id),
    KEY idx_residentes_apartamento (apartamento_id),
    CONSTRAINT fk_residentes_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id),
    CONSTRAINT fk_residentes_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB;

CREATE TABLE reservas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    residente_id BIGINT NULL,
    tipo_espacio ENUM('BBQ', 'GIMNASIO', 'PISCINA', 'SALON_SOCIAL') NOT NULL,
    fecha_inicio DATETIME(6) NOT NULL,
    fecha_fin DATETIME(6) NULL,
    observaciones VARCHAR(255) NULL,
    estado ENUM('APROBADA', 'PENDIENTE', 'RECHAZADA') NULL,
    creado_en DATETIME(6) NULL,
    apartamento_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_reservas_residente (residente_id),
    KEY idx_reservas_apartamento (apartamento_id),
    CONSTRAINT fk_reservas_residente
        FOREIGN KEY (residente_id) REFERENCES residentes (id),
    CONSTRAINT fk_reservas_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB;

CREATE TABLE pagos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    residente_id BIGINT NULL,
    monto DECIMAL(38, 2) NOT NULL,
    metodo ENUM('EFECTIVO', 'PSE', 'TARJETA', 'TRANSFERENCIA') NULL,
    fecha DATE NOT NULL,
    estado_pago ENUM('PAGADO', 'PENDIENTE', 'VENCIDO') NOT NULL,
    tipo_pago ENUM('ADMINISTRACION', 'MULTA', 'OTRO') NULL,
    fecha_vencimiento DATE NULL,
    referencia_pago VARCHAR(255) NULL,
    creado_en DATETIME(6) NULL,
    apartamento_id BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pagos_referencia (referencia_pago),
    KEY idx_pagos_residente (residente_id),
    KEY idx_pagos_apartamento (apartamento_id),
    CONSTRAINT fk_pagos_residente
        FOREIGN KEY (residente_id) REFERENCES residentes (id),
    CONSTRAINT fk_pagos_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB;

CREATE TABLE visitantes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NOT NULL,
    documento VARCHAR(255) NOT NULL,
    fecha_entrada DATETIME(6) NULL,
    fecha_salida DATETIME(6) NULL,
    apartamento_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_visitantes_apartamento (apartamento_id),
    CONSTRAINT fk_visitantes_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB;
