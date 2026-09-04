-- Esquema base de nexur_db. Reproduce el estado previo a Flyway para poder
-- levantar la aplicacion sobre una base de datos vacia (Aiven u otro entorno).
-- Las columnas que agregan las migraciones posteriores no se incluyen aqui:
--   residentes.correo y usuario.debe_cambiar_password las crea V2.

CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE apartamentos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(255) NULL,
    torre VARCHAR(255) NULL,
    piso INT NULL,
    estado VARCHAR(255) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE residentes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NULL,
    documento VARCHAR(255) NULL,
    telefono VARCHAR(255) NULL,
    usuario_id BIGINT NULL,
    apartamento_id BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_residentes_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_residentes_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario (id),
    CONSTRAINT fk_residentes_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE pagos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    residente_id BIGINT NULL,
    apartamento_id BIGINT NULL,
    monto DECIMAL(38,2) NOT NULL,
    metodo VARCHAR(20) NULL,
    fecha DATE NULL,
    estado_pago VARCHAR(20) NOT NULL,
    tipo_pago VARCHAR(20) NULL,
    fecha_vencimiento DATE NULL,
    referencia_pago VARCHAR(255) NULL,
    creado_en DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_pagos_referencia UNIQUE (referencia_pago),
    KEY idx_pagos_fecha (fecha),
    KEY idx_pagos_estado (estado_pago),
    CONSTRAINT fk_pagos_residente
        FOREIGN KEY (residente_id) REFERENCES residentes (id),
    CONSTRAINT fk_pagos_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE reservas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    residente_id BIGINT NULL,
    apartamento_id BIGINT NULL,
    tipo_espacio VARCHAR(30) NULL,
    fecha_inicio DATETIME(6) NULL,
    fecha_fin DATETIME(6) NULL,
    observaciones VARCHAR(255) NULL,
    estado VARCHAR(20) NULL,
    creado_en DATETIME(6) NULL,
    PRIMARY KEY (id),
    KEY idx_reservas_fecha_inicio (fecha_inicio),
    CONSTRAINT fk_reservas_residente
        FOREIGN KEY (residente_id) REFERENCES residentes (id),
    CONSTRAINT fk_reservas_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE visitantes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(255) NULL,
    documento VARCHAR(255) NULL,
    fecha_entrada DATETIME(6) NULL,
    fecha_salida DATETIME(6) NULL,
    apartamento_id BIGINT NULL,
    PRIMARY KEY (id),
    KEY idx_visitantes_fecha_entrada (fecha_entrada),
    CONSTRAINT fk_visitantes_apartamento
        FOREIGN KEY (apartamento_id) REFERENCES apartamentos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
