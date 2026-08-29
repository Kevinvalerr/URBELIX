CREATE TABLE IF NOT EXISTS password_reset_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(100) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_password_reset_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

ALTER TABLE apartamentos ADD COLUMN IF NOT EXISTS codigo_registro VARCHAR(32) UNIQUE;

-- Compatibilidad de la base H2 existente tras incorporar la desactivacion de usuarios.
-- El valor por defecto conserva habilitadas las cuentas creadas antes de esta migracion.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS debe_cambiar_password BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS avisos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(120) NOT NULL,
    contenido VARCHAR(2000) NOT NULL,
    publicado_en TIMESTAMP NOT NULL,
    vence_en TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_avisos_visibles ON avisos (activo, vence_en, publicado_en);

CREATE TABLE IF NOT EXISTS incidencia_comentario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contenido VARCHAR(2000) NOT NULL,
    autor_nombre VARCHAR(160) NOT NULL,
    autor_email VARCHAR(180) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    incidencia_id BIGINT NOT NULL,
    CONSTRAINT fk_comentario_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencia(id)
);

CREATE TABLE IF NOT EXISTS notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(120) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    enlace VARCHAR(255),
    creada_en TIMESTAMP NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    usuario_id BIGINT NOT NULL,
    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_notificaciones_usuario ON notificaciones (usuario_id, leida, creada_en);

CREATE TABLE IF NOT EXISTS incidencia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asunto VARCHAR(120) NOT NULL,
    descripcion VARCHAR(2000) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    respuesta VARCHAR(2000),
    residente_id BIGINT NOT NULL,
    apartamento_id BIGINT,
    creado_en TIMESTAMP NOT NULL,
    actualizado_en TIMESTAMP,
    CONSTRAINT fk_incidencia_residente FOREIGN KEY (residente_id) REFERENCES residentes(id),
    CONSTRAINT fk_incidencia_apartamento FOREIGN KEY (apartamento_id) REFERENCES apartamentos(id)
);

CREATE TABLE IF NOT EXISTS incidencia_adjunto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_interno VARCHAR(80) NOT NULL UNIQUE,
    tipo_contenido VARCHAR(100) NOT NULL,
    tamano BIGINT NOT NULL,
    cargado_por VARCHAR(180) NOT NULL,
    creado_en TIMESTAMP NOT NULL,
    incidencia_id BIGINT NOT NULL,
    CONSTRAINT fk_adjunto_incidencia FOREIGN KEY (incidencia_id) REFERENCES incidencia(id)
);

CREATE INDEX IF NOT EXISTS idx_adjunto_incidencia ON incidencia_adjunto (incidencia_id, creado_en);

CREATE TABLE IF NOT EXISTS pago_webhook_evento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evento_id VARCHAR(120) NOT NULL UNIQUE,
    referencia_pago VARCHAR(255) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    recibido_en TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_webhook_referencia ON pago_webhook_evento (referencia_pago, recibido_en);

ALTER TABLE pagos ADD COLUMN IF NOT EXISTS fecha_pago DATE;

CREATE TABLE IF NOT EXISTS auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_email VARCHAR(180) NOT NULL,
    accion VARCHAR(120) NOT NULL,
    entidad VARCHAR(255) NOT NULL,
    entidad_id BIGINT,
    detalle VARCHAR(500),
    creado_en TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_auditoria_fecha ON auditoria (creado_en);
CREATE INDEX IF NOT EXISTS idx_auditoria_actor ON auditoria (actor_email, creado_en);

CREATE TABLE IF NOT EXISTS parqueaderos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(255) NOT NULL UNIQUE,
    zona VARCHAR(255),
    estado VARCHAR(20) NOT NULL,
    tipo VARCHAR(20) NOT NULL DEFAULT 'CARRO',
    apartamento_id BIGINT,
    vehiculo_id BIGINT UNIQUE,
    CONSTRAINT fk_parqueaderos_apartamento FOREIGN KEY (apartamento_id) REFERENCES apartamentos(id)
);

CREATE TABLE IF NOT EXISTS vehiculos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    placa VARCHAR(10) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL,
    marca VARCHAR(255),
    modelo VARCHAR(255),
    color VARCHAR(255),
    residente_id BIGINT NOT NULL,
    CONSTRAINT fk_vehiculos_residente FOREIGN KEY (residente_id) REFERENCES residentes(id)
);

CREATE TABLE IF NOT EXISTS movimientos_parqueadero (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vehiculo_id BIGINT NOT NULL,
    parqueadero_id BIGINT NOT NULL,
    fecha_hora_ingreso TIMESTAMP NOT NULL,
    fecha_hora_salida TIMESTAMP,
    estado VARCHAR(20) NOT NULL,
    CONSTRAINT fk_movimientos_vehiculo FOREIGN KEY (vehiculo_id) REFERENCES vehiculos(id),
    CONSTRAINT fk_movimientos_parqueadero FOREIGN KEY (parqueadero_id) REFERENCES parqueaderos(id)
);
