# Pruebas y despliegue de URBELIX

Este documento organiza las validaciones que deben hacerse antes de declarar una
version final. Las pruebas pesadas no se ejecutan en cada cambio local.

## Estado comprobado 29/08/2026

- `.\mvnw.cmd clean test`: 113 pruebas, 0 fallos y 0 errores.
- El perfil `dev` arranca con H2 y el login del administrador funciona por HTTP.
- Smoke HTTP local: ADMIN, RESIDENTE y PORTERIA recorren sus rutas principales; las rutas no autorizadas responden `403` y los formularios POST incluyen CSRF.
- Pago simulado PSE: ADMIN crea la obligacion, RESIDENTE inicia el checkout, procesa `APPROVED` y descarga la factura PDF.
- La suite automatizada no sustituye la validacion `prod` con MySQL/Flyway, las pruebas
  de navegador, SMTP/Wompi reales ni carga/estres.

## Niveles de prueba

### Unitarias

- Servicios de usuarios, visitantes, reservas, pagos, parqueaderos e incidencias.
- Validaciones de contrasena, correo, documento, transiciones de estado y filtros.
- Pagos en línea PSE/tarjeta: el flujo manual no puede confirmar el pago; la confirmacion depende del checkout y webhook validado.
- Pagos por transferencia/efectivo: solo ADMIN puede confirmarlos y se registra la fecha efectiva de pago.
- Factura individual: cada pago autorizado genera un PDF descargable, incluso si es histórico o sigue pendiente.
- Recuperacion de contrasena: formulario con CSRF, token de un solo uso y servicio SMTP preparado; el envio real con Gmail sigue pendiente de validacion operativa.
- Sandbox local de pagos: aprobacion, pendiente, rechazo, error, monto, referencia,
  checksum e idempotencia usando el mismo procesador Wompi.
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
2. Abrir un pago PSE o tarjeta pendiente y pulsar `Preparar pago en sandbox`.
3. Entrar a `Abrir checkout de prueba`.
4. Ejecutar `APPROVED` y confirmar que el pago queda `PAGADO`.
5. Repetir con `PENDING`, `DECLINED`, `VOIDED` y `ERROR` sobre otros pagos y confirmar
   que permanecen pendientes.
6. Abrir un pago histórico autorizado y descargar `Factura`; validar que el PDF incluya
   emisión, vencimiento, fecha efectiva de pago, referencia, método y estado.
7. Solicitar recuperación desde `/forgot-password` con una cuenta de prueba y validar
   que el mensaje llegue por SMTP, que el enlace abra `/reset-password` y que el token
   no pueda reutilizarse después del cambio.

El sandbox local no representa una autorización bancaria real. La prueba del
proveedor externo requiere las credenciales Wompi, una URL HTTPS pública para
el webhook y una transacción en el ambiente sandbox del proveedor.

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

La imagen final debe separar estos servicios:

- `app`: Spring Boot, sin secretos en la imagen.
- `db`: MySQL 8 con volumen persistente y usuario de aplicacion con permisos minimos.
- `reportes`: FastAPI/ReportLab, sin SQLite de negocio.

El despliegue debe usar variables de entorno para base de datos, SMTP, Wompi,
PSE y URLs internas. Antes de crear el `docker-compose.yml` definitivo hay que
cerrar las pruebas de integracion y definir los healthchecks, limites de memoria,
red interna, backups y estrategia de migraciones Flyway.

## Criterio de salida

No se declara version final hasta que pasen `clean test`, las pruebas manuales
por cada rol, la validacion MySQL/Flyway, el flujo FastAPI opcional, la prueba de
carga y una restauracion comprobada del backup.
