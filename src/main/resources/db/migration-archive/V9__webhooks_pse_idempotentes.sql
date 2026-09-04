-- URBELIX - Eventos PSE recibidos de forma idempotente
CREATE TABLE pago_webhook_evento (
    id BIGINT NOT NULL AUTO_INCREMENT,
    evento_id VARCHAR(120) NOT NULL,
    referencia_pago VARCHAR(255) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    recibido_en DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_webhook_evento (evento_id)
) ENGINE=InnoDB;

CREATE INDEX idx_webhook_referencia ON pago_webhook_evento (referencia_pago, recibido_en);
