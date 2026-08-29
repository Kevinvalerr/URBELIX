# URBELIX

Sistema web para la gestion de conjuntos residenciales.

## Requisitos

- Java 21 o superior compatible con el proyecto.
- Maven Wrapper incluido: `mvnw.cmd` para Windows o `./mvnw` para Linux/macOS.
- MySQL 8 para el perfil `prod`.
- Cuenta SMTP y credenciales del proveedor de pagos solo si se prueban esos flujos.
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

## Ejecutar pruebas

Las pruebas usan H2 en memoria y no necesitan MySQL, SMTP ni Wompi. La suite actual cuenta con 113 pruebas.

```powershell
.\mvnw.cmd clean test
```

## Sandbox local de pagos

En el perfil `dev`, el sandbox local de pagos está activo por defecto mediante
`PAYMENTS_SIMULATION_ENABLED=true`. El flujo es: un administrador crea un pago
PSE o tarjeta, el residente pulsa `Preparar pago en sandbox`, abre `Abrir checkout de prueba` y
elige un resultado. La aplicación genera una transacción de prueba firmada y la
procesa con el mismo validador de evento Wompi, incluyendo referencia, monto,
checksum e idempotencia. Se pueden probar estados aprobados, pendientes,
rechazados, anulados y con error. No se realiza ningún cobro ni se contacta al proveedor.
El sandbox usa `PAYMENTS_SIMULATION_SECRET`, un secreto local separado de
`WOMPI_EVENTS_SECRET`.

Cada pago tiene fecha de emisión, vencimiento y fecha efectiva de pago. El residente
puede abrir el detalle y descargar la factura/comprobante PDF de cualquier registro
histórico que le pertenezca, mientras que el administrador puede hacerlo para todos.
Transferencia y efectivo se confirman únicamente desde administración; PSE y tarjeta
se confirman por el checkout y su evento validado.

Para desactivarlo en desarrollo:

```powershell
$env:PAYMENTS_SIMULATION_ENABLED = "false"
```

En `prod` está desactivado por defecto y solo se permite el flujo real con las
credenciales de Wompi configuradas.

## Perfil de produccion

El perfil `prod` usa MySQL, valida el esquema con Hibernate y ejecuta las
migraciones Flyway de `src/main/resources/db/migration`.

La validacion automatizada actual se ejecuta sobre H2. La validacion de arranque
con MySQL/Flyway, el envio SMTP real, el proveedor Wompi, las pruebas de navegador
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
- `PSE_WEBHOOK_SECRET`.
- `WOMPI_BASE_URL`, `WOMPI_PUBLIC_KEY`, `WOMPI_PRIVATE_KEY`.
- `WOMPI_INTEGRITY_SECRET`, `WOMPI_EVENTS_SECRET`.

Para la preparacion de MySQL consultar [MYSQL_MIGRATION.md](MYSQL_MIGRATION.md).
Para Wompi consultar [WOMPI_SETUP.md](WOMPI_SETUP.md).

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

La matriz de requisitos y el estado actual estan en
[REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md) y
[PROJECT_STATUS.md](PROJECT_STATUS.md).

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
