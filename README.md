# URBELIX

Sistema web para la gestion de conjuntos residenciales.

## Requisitos

- Java 21 o superior compatible con el proyecto.
- Maven Wrapper incluido: `mvnw.cmd` para Windows o `./mvnw` para Linux/macOS.
- MySQL 8 para el perfil `prod`.
- Cuenta SMTP solo si se prueban los correos reales de recuperacion y avisos.
- Python 3.12 o superior compatible y `requirements-fastapi.txt` solo si se habilita el proveedor opcional de reportes.

## Ejecutar en desarrollo

El perfil `dev` usa una base H2 local en `./data/nexurdb` y crea datos de prueba.

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicacion queda disponible en `http://localhost:8080`.

Credenciales de desarrollo:

- Administrador: `DEV_ADMIN_EMAIL` y `DEV_ADMIN_PASSWORD`.
- Porteria: `porteria@nexur.com` y `DEV_PORTERIA_PASSWORD`.

Los valores predeterminados de desarrollo estan definidos en
`src/main/resources/application-dev.properties`. Deben cambiarse antes de
compartir una instancia fuera del equipo local.

Si se pierde la clave ADMIN de MySQL, existe una recuperacion controlada para
un solo arranque. Se activa temporalmente con `ADMIN_BOOTSTRAP_ENABLED=true`,
`ADMIN_BOOTSTRAP_EMAIL` y `ADMIN_BOOTSTRAP_PASSWORD`; la cuenta queda obligada
a cambiar esa clave al ingresar. Desactiva la variable despues de recuperar el
acceso y nunca guardes la clave en Git.

## Ejecutar con Docker

Docker Compose levanta Spring Boot con el perfil `prod`, MySQL 8.4 y el servicio
opcional de reportes FastAPI. Flyway crea el esquema base y ejecuta las
migraciones disponibles al iniciar la aplicacion.

```powershell
Copy-Item .env.example .env
# Edita .env y cambia como minimo MYSQL_PASSWORD y MYSQL_ROOT_PASSWORD.
docker compose --env-file .env up -d --build
docker compose --env-file .env ps
```

La aplicacion queda disponible en `http://localhost:8080`. Para la primera
instalacion, habilita temporalmente `ADMIN_BOOTSTRAP_ENABLED=true` en `.env`,
define `ADMIN_BOOTSTRAP_EMAIL` y `ADMIN_BOOTSTRAP_PASSWORD`, inicia los servicios,
entra al sistema, cambia la clave y vuelve a desactivar esa variable.

Los datos de MySQL y los archivos subidos se conservan en volumenes Docker.
Para detener los contenedores sin borrar datos usa `docker compose stop` o
`docker compose down`. No uses `docker compose down -v` salvo que quieras borrar
la base de datos y los archivos persistidos.

Para un servidor publico, cambia `APP_BASE_URL` a HTTPS, activa
`SESSION_COOKIE_SECURE=true`, configura un proxy TLS y completa las credenciales
SMTP en el entorno del servidor. Consulta `.env.example` como plantilla;
no subas `.env` al repositorio.

Para desplegar en un VPS con HTTPS automatico, consulta
[`VPS_DEPLOYMENT.md`](VPS_DEPLOYMENT.md) y usa la composicion adicional
`docker-compose.prod.yml`.

## Ejecutar pruebas

Las pruebas usan H2 en memoria y no necesitan MySQL, SMTP ni proveedores de pago externos.

```powershell
.\mvnw.cmd clean test
```

## Sandbox local de pagos

En el perfil `dev`, el sandbox local de pagos está activo por defecto mediante
`PAYMENTS_SIMULATION_ENABLED=true`. Un administrador crea un pago PSE o tarjeta,
el residente pulsa `Preparar pago simulado`, abre el simulador local y elige un
resultado. Solo una aprobación cambia el estado a pagado y registra la fecha
efectiva. Se pueden probar estados aprobados, pendientes, rechazados, anulados y
con error. No se realiza ningún cobro ni se contacta a un proveedor.

Cada pago tiene fecha de emisión, vencimiento y fecha efectiva de pago. El residente
puede abrir el detalle y descargar la factura/comprobante PDF de cualquier registro
histórico que le pertenezca, mientras que el administrador puede hacerlo para todos.
Transferencia y efectivo se confirman únicamente desde administración; PSE y tarjeta
se confirman desde el simulador local.

Para desactivarlo en desarrollo:

```powershell
$env:PAYMENTS_SIMULATION_ENABLED = "false"
```

En `prod` también puede desactivarse mediante `PAYMENTS_SIMULATION_ENABLED=false`.

## Perfil de produccion

El perfil `prod` usa MySQL, valida el esquema con Hibernate y ejecuta las
migraciones Flyway de `src/main/resources/db/migration`.

La validacion automatizada actual se ejecuta sobre H2. La validacion de arranque
con MySQL/Flyway, el envio SMTP real, las pruebas de navegador
y la carga/estres deben repetirse antes de declarar el despliegue listo.

Antes de iniciar, configurar las variables sin guardarlas en Git:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:mysql://localhost:3306/nexur_db"
$env:DB_USERNAME = "usuario_de_aplicacion"
$env:DB_PASSWORD = "contraseña_local"
$env:APP_BASE_URL = "http://localhost:8080"
```

Variables opcionales:

- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.
- `NOTIFICATIONS_EMAIL_ENABLED`.
- `URBELIX_FASTAPI_URL`, `REPORTS_FASTAPI_ENABLED`.

Para la preparacion de MySQL consultar [MYSQL_MIGRATION.md](MYSQL_MIGRATION.md).
Para pagos simulados consultar [PAYMENTS_SIMULATION.md](PAYMENTS_SIMULATION.md).

## Modulos y roles

- `ADMIN`: administra usuarios, residentes, apartamentos, pagos, reservas,
  avisos, incidencias, espacios de parqueadero, reportes y auditoria. No opera
  las entradas y salidas de porteria.
- `RESIDENTE`: consulta su informacion, crea reservas, consulta sus pagos,
  registra sus propios vehiculos, crea incidencias propias y envia solicitudes
  de acceso para sus visitantes. No aprueba accesos ni registra entradas o
  salidas.
- `PORTERIA`: opera exclusivamente el control de acceso: revisa solicitudes,
  aprueba o rechaza visitantes, registra entradas y salidas, y administra el
  catalogo operativo de vehiculos y movimientos de parqueadero. No tiene
  apartamento ni perfil residencial.

Las restricciones se aplican en `SecurityConfig` y tambien en los
controladores mediante `@PreAuthorize`.

La administracion puede importar residentes desde Excel en
`/residentes/importar`. El archivo exige nombre, documento, telefono, correo,
apartamento, torre, piso y codigo de registro. Las filas se validan de forma
independiente, se auditan y las cuentas reciben una contrasena temporal
aleatoria; las credenciales solo se envian si SMTP esta habilitado.

### Flujo de visitantes

1. El residente envia una solicitud con el visitante; el apartamento se toma
   automaticamente de su perfil.
2. Porteria revisa la solicitud y la aprueba o la rechaza con motivo.
3. Solo porteria registra la entrada de una solicitud aprobada y su salida.
4. Los estados son `PENDIENTE`, `APROBADA`, `RECHAZADA`, `DENTRO` y
   `FINALIZADA`.

## Flujo Scrum

1. Crear una rama desde `main` con el identificador de la historia.
2. Ejecutar `clean test` antes de abrir el Pull Request.
3. Describir en el Pull Request la historia, criterios de aceptacion, pruebas
   realizadas y cualquier variable externa requerida.
4. No subir contraseñas, claves API, tokens, bases H2 ni archivos de `target`.

La especificacion completa de requisitos funcionales, no funcionales, permisos y
flujos de aceptacion esta en [REQUISITOS_URBELIX.md](REQUISITOS_URBELIX.md).
El resumen de trazabilidad esta en [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md)
y el estado del proyecto en [PROJECT_STATUS.md](PROJECT_STATUS.md).

## Reportes y servicios externos

El PDF de reportes se genera por defecto desde Spring Boot. Para usar el
microservicio FastAPI/ReportLab, inicia `fastapi_reportes.py` y define
`REPORTS_FASTAPI_ENABLED=true`; Spring le envia los datos y mantiene el PDF
local como respaldo. FastAPI no persiste datos de negocio.

Configuracion local del proveedor opcional:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-fastapi.txt
$env:REPORTS_FASTAPI_ENABLED = "true"
.\.venv\Scripts\python.exe -m uvicorn fastapi_reportes:app --host 127.0.0.1 --port 8000
```

En otra terminal se inicia Spring Boot. La verificacion local confirma `/health`
y la generacion de un PDF desde Spring Boot hacia FastAPI.
