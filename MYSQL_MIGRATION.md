# Migracion MySQL de URBELIX

El perfil `prod` usa `spring.jpa.hibernate.ddl-auto=validate` y aplica las migraciones versionadas con Flyway. La aplicacion registra cada version ejecutada en `flyway_schema_history`.

Migraciones versionadas incluidas:

- `src/main/resources/db/migration/V0_1__esquema_base.sql`
- `src/main/resources/db/migration/V1__urbelix_funcionalidades_nuevas.sql`
- `src/main/resources/db/migration/V2__parqueaderos_vehiculos_movimientos.sql`
- `src/main/resources/db/migration/V3__usuarios_activos.sql`
- `src/main/resources/db/migration/V4__avisos_generales.sql`
- `src/main/resources/db/migration/V5__cambio_password_inicial.sql`
- `src/main/resources/db/migration/V6__comentarios_incidencias.sql`
- `src/main/resources/db/migration/V7__notificaciones_internas.sql`
- `src/main/resources/db/migration/V8__evidencias_incidencias.sql`
- `src/main/resources/db/migration/V9__webhooks_pse_idempotentes.sql`
- `src/main/resources/db/migration/V10__auditoria_mutaciones.sql`
- `src/main/resources/db/migration/V11__solicitudes_visitantes_por_rol.sql`
- `src/main/resources/db/migration/V12__fecha_pago_facturacion.sql`
- `src/main/resources/db/migration/V13__retirar_integracion_pago_externa.sql`
- `src/main/resources/db/migration/V14__trazabilidad_sandbox_pagos.sql`

## Instalaciones nuevas

Las instalaciones nuevas no necesitan crear manualmente las tablas base. La
migracion `V0_1__esquema_base.sql` crea `usuario`, `apartamentos`, `residentes`,
`reservas`, `pagos` y `visitantes`; luego Flyway aplica V1-V14 en orden.

## Prevalidacion de bases existentes

1. Realizar un respaldo completo de la base de datos.
2. Detener la aplicacion para evitar escrituras durante la migracion.
3. Confirmar que existen las tablas `usuario`, `apartamentos` y `residentes`.
4. Confirmar que el esquema base ya contiene `usuario`, `apartamentos` y `residentes`.

## Ejecucion recomendada

Configurar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, y arrancar con el perfil `prod`. Flyway ejecutara automaticamente:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
```

No guardar contrasenas en el comando ni en el repositorio. No ejecutar tambien el SQL manualmente, porque se produciria una migracion duplicada.

Si se pierde la clave de ADMIN, se puede usar temporalmente la recuperacion controlada de la
aplicacion definiendo `ADMIN_BOOTSTRAP_ENABLED=true`, `ADMIN_BOOTSTRAP_EMAIL` y
`ADMIN_BOOTSTRAP_PASSWORD` en el entorno del proceso. La clave debe cumplir la politica de
seguridad, la cuenta queda obligada a cambiarla al ingresar y `ADMIN_BOOTSTRAP_ENABLED` debe
volver a `false` despues de un arranque exitoso. No se promueve una cuenta existente de otro rol.

Las evidencias se guardan en el directorio indicado por `UPLOAD_DIR` (por defecto `./data/uploads`). En produccion debe apuntar a un volumen privado con respaldo y permisos de escritura para la aplicacion.

Los pagos no exponen webhooks ni integraciones externas. PSE y tarjeta se recorren
en el sandbox local controlado por la aplicacion; transferencia y efectivo solo
pueden ser confirmados por ADMIN. La migracion V13 retira la tabla historica de
eventos externos y V14 agrega resultado, transaccion y fecha de simulacion.

Si la organizacion no permite que la aplicacion ejecute migraciones, se puede aplicar el archivo SQL manualmente antes del primer arranque y luego registrar la version en Flyway siguiendo un procedimiento de base de datos controlado. No se deben combinar ambos metodos sin actualizar el historial de Flyway.

## Verificacion

```sql
SHOW COLUMNS FROM apartamentos LIKE 'codigo_registro';
SHOW TABLES LIKE 'password_reset_token';
SHOW TABLES LIKE 'incidencia';
SHOW TABLES LIKE 'parqueaderos';
SHOW TABLES LIKE 'vehiculos';
SHOW TABLES LIKE 'movimientos_parqueadero';
SHOW TABLES LIKE 'incidencia_adjunto';
SHOW TABLES LIKE 'auditoria';
SHOW COLUMNS FROM usuario LIKE 'activo';
SHOW COLUMNS FROM pagos LIKE 'fecha_pago';
SHOW COLUMNS FROM pagos LIKE 'resultado_simulacion';
SHOW COLUMNS FROM pagos LIKE 'transaccion_simulada';
SHOW COLUMNS FROM pagos LIKE 'simulado_en';
```

Luego iniciar con el perfil `prod` y revisar `flyway_schema_history`. Si Flyway o JPA informa una diferencia, detener el despliegue y revisar el esquema antes de cambiar `ddl-auto`; no se debe usar `update` en produccion.

La validacion local del 03/09/2026 conecto correctamente con MySQL 8.4 en
`nexur_db`, valido y aplico V0.1-V14; `flyway_schema_history` quedo en la
version 14. V12 agrega `pagos.fecha_pago`, V13 retira la integracion externa
obsoleta y V14 agrega trazabilidad del sandbox. Para el despliegue final se debe
repetir el respaldo y usar un usuario MySQL exclusivo de la aplicacion, no `root`.
