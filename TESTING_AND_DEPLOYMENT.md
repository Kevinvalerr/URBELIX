# Pruebas y despliegue de URBELIX

Este documento organiza las validaciones que deben hacerse antes de declarar una
version final. Las pruebas pesadas no se ejecutan en cada cambio local.

## Estado comprobado 03/09/2026

- `.\mvnw.cmd clean test`: 185 pruebas, 0 fallos y 0 errores.
- JaCoCo: paquete `service` con `90,2%` de lineas y `72,3%` de ramas; supera los umbrales de `80%` y `70%`.
- El perfil `dev` arranca con H2 y el login del administrador funciona por HTTP.
- Smoke HTTP local: ADMIN, RESIDENTE y PORTERIA recorren sus rutas principales; las rutas no autorizadas responden `403` y los formularios POST incluyen CSRF.
- Pago simulado PSE: ADMIN crea la obligacion, RESIDENTE inicia el checkout, procesa `APPROVED` y descarga la factura PDF.
- La suite automatizada no sustituye la validacion `prod` con MySQL/Flyway, las pruebas
  de navegador, SMTP real ni carga/estres.

## Niveles de prueba

### Unitarias

- Servicios de usuarios, visitantes, reservas, pagos, parqueaderos e incidencias.
- Validaciones de contrasena, correo, documento, transiciones de estado y filtros.
- Pagos en línea PSE/tarjeta: el residente usa el sandbox local; solo el resultado `APPROVED` confirma el pago y guarda su trazabilidad.
- Pagos por transferencia/efectivo: solo ADMIN puede confirmarlos y se registra la fecha efectiva de pago.
- Factura individual: cada pago autorizado genera un PDF descargable, incluso si es histórico o sigue pendiente.
- Recuperacion de contrasena: formulario con CSRF, token de un solo uso y servicio SMTP preparado; el envio real con Gmail sigue pendiente de validacion operativa.
- Sandbox local de pagos: aprobacion, pendiente, rechazo, anulado y error,
  con referencia, transacción simulada y fecha persistidas.
- Importacion Excel: cabecera, filas duplicadas, apartamento/codigo inconsistente,
  limite de 1000 filas y creacion de cuenta temporal.

Comando base:

```powershell
.\mvnw.cmd -Dtest=*ServiceTest test
```

### Integracion

- Spring Boot con H2 para validar rutas, CSRF, roles, persistencia y plantillas.
- Perfil `prod` contra MySQL con Flyway y Hibernate en `validate`.
- FastAPI con `/health` y generacion de PDF cuando
  `REPORTS_FASTAPI_ENABLED=true`.

Comandos base:

```powershell
.\mvnw.cmd -Dtest=NexurIntegrationTest test
.\mvnw.cmd clean test
python -m uvicorn fastapi_reportes:app --host 127.0.0.1 --port 8000
```

Prueba funcional local de pagos:

1. Iniciar el perfil `dev` y entrar como residente.
2. Abrir un pago PSE o tarjeta pendiente y pulsar `Pagar`.
3. Entrar a `Confirmar resultado simulado`.
4. Ejecutar `APPROVED` y confirmar que el pago queda `PAGADO`.
5. Repetir con `PENDING`, `DECLINED`, `VOIDED` y `ERROR` sobre otros pagos y confirmar
   que permanecen pendientes.
6. Abrir un pago histórico autorizado y descargar `Factura`; validar que el PDF incluya
   emisión, vencimiento, fecha efectiva de pago, referencia, método y estado.
7. Solicitar recuperación desde `/forgot-password` con una cuenta de prueba y validar
   que el mensaje llegue por SMTP, que el enlace abra `/reset-password` y que el token
   no pueda reutilizarse después del cambio.

El sandbox local no representa una autorización bancaria real. No existen
credenciales, webhooks ni llamadas a un proveedor externo en este alcance.

Para reproducir la prueba en Windows usando el entorno aislado del proyecto:

```powershell
.\.venv\Scripts\python.exe -m uvicorn fastapi_reportes:app --host 127.0.0.1 --port 8000
```

Verificacion realizada localmente el 28/08/2026: `/health` respondio `status=ok`
y Spring Boot, autenticado como ADMIN, recibio un PDF `200 application/pdf` desde
FastAPI con `REPORTS_FASTAPI_ENABLED=true`.

### Carga y estres

Todavia pendientes. Se debe usar un escenario reproducible con una herramienta
como k6 o JMeter y registrar version, hardware, base de datos, tamano de datos,
usuarios concurrentes, latencias p50/p95/p99, errores y consumo de memoria.

Minimos a validar:

- 100 usuarios concurrentes como referencia del requisito RNF-04.
- Login y dashboard.
- Consulta de pagos e incidencias.
- Creacion de reservas y solicitudes de visitantes.
- Generacion de reportes sin bloquear las peticiones normales.

## Decision sobre relaciones

La rama de referencia incluye una tabla de asociacion para permitir varios
apartamentos por residente. `URBELIXXX` conserva por ahora la relacion
residente-apartamento unica porque pagos, reservas, visitantes y parqueaderos
la usan directamente. La asociacion multiple solo se debe incorporar con una
migracion coordinada, datos historicos y pruebas de propiedad por apartamento;
copiar solo la tabla dejaria consultas y permisos inconsistentes.

## Preparacion para Docker

La composicion Docker disponible en el repositorio separa estos servicios:

- `app`: Spring Boot con el perfil `prod`, sin secretos en la imagen.
- `db`: MySQL 8.4 con volumen persistente y usuario de aplicacion.
- `reportes`: FastAPI/ReportLab, sin SQLite de negocio.

Para levantarla localmente:

```powershell
Copy-Item .env.example .env
# Cambiar secretos y, para el primer acceso, configurar el bootstrap ADMIN.
docker compose --env-file .env up -d --build
docker compose --env-file .env ps
docker compose --env-file .env logs -f app
```

`db` tiene healthcheck y `app` espera a que MySQL y FastAPI estén saludables.
Flyway ejecuta las migraciones al iniciar Spring Boot. Los datos se guardan en
los volumenes `mysql-data` y `app-data`; no se debe usar `down -v` en un entorno
con datos importantes.

La configuracion usa variables de entorno para base de datos, SMTP, reportes,
URLs internas y cookies. En un servidor publico se debe usar HTTPS, configurar
un proxy TLS, cambiar `SESSION_COOKIE_SECURE` a `true`, limitar recursos y
definir backups antes de declarar el despliegue operativo.

La composicion queda lista para validacion local y despliegue controlado, pero
aun requiere pruebas contra el MySQL de aceptación, restauración de backups,
carga y SMTP real antes de una version productiva.

## Criterio de salida

La version candidata queda funcional para demostración y aceptación. Para una
salida productiva todavía deben completarse `clean test`, las pruebas manuales
por cada rol, la validacion MySQL/Flyway, la prueba de carga y una restauracion
comprobada del backup; FastAPI es opcional porque existe un generador local.
