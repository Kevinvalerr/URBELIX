# URBELIX

Sistema web de gestion residencial con Spring Boot, Thymeleaf, MariaDB y FastAPI.

## Inicio

Requisitos: Java 21, MariaDB/XAMPP y Python 3.12 con dependencias de `requirements-fastapi.txt`.

Spring Boot:

```powershell
.\mvnw.cmd spring-boot:run
```

Por defecto usa el puerto 8080 y la base existente `nexur_db` en `localhost:3306`.

FastAPI, desde esta carpeta:

```powershell
python -m uvicorn fastapi_reportes:app --host 0.0.0.0 --port 8000
```

Health check: `GET http://localhost:8000/health`.

## Configuracion

La conexion MariaDB se configura en `src/main/resources/application.properties`. SMTP usa variables de entorno `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` y `MAIL_PASSWORD`; no se deben guardar secretos en el repositorio.

## Migraciones

Flyway administra el esquema. El proyecto usa `spring.jpa.hibernate.ddl-auto=validate`. Las migraciones estan en `src/main/resources/db/migration` y actualmente incluyen baseline v1, preparacion de residentes/asociaciones v2, auditoria v3 e incidencias v4.

Antes de migrar:

1. Crear backup completo con `mysqldump`.
2. Verificar tamaño, contenido y SHA-256.
3. Probar restauracion en una base temporal.
4. Aplicar migraciones y validar conteos/relaciones.

## Funcionalidades

- Residentes: alta con correo, cuenta automatica y uno o varios apartamentos.
- Excel: plantilla, importacion por filas, validaciones y resumen.
- Usuarios: consulta administrativa; las cuentas RESIDENTE nacen desde Residentes.
- Contraseñas: BCrypt, temporal en memoria y cambio obligatorio.
- Pagos: estados, simulacion aprobada/rechazada, referencias unicas y recibo PDF.
- Incidencias: CRUD, filtros, estados controlados, historial, auditoría y notificaciones.
- Reportes: Spring Boot solicita PDF a FastAPI/ReportLab.
- Auditoria: eventos de operaciones criticas sin secretos.

FastAPI expone `GET /health`, CRUD de `/incidencias` y `POST /reportes/generar-pdf`. El CRUD Python usa SQLite local (`urbelix_fastapi.sqlite3`) y el dominio principal de incidencias se persiste en MariaDB mediante la migración Flyway V4.

Las notificaciones SMTP de incidencias usan `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME` y `MAIL_PASSWORD`. Los resultados comprobados y pendientes estan en `IMPLEMENTACION_FINAL_URBELIX.md`.
