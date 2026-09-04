# URBELIX - Estado del proyecto

## Estado actual

La especificacion fuente de verdad para requisitos y pruebas esta en
[REQUISITOS_URBELIX.md](REQUISITOS_URBELIX.md). Los estados de ese documento
distinguen implementacion comprobada, cobertura parcial y pendientes reales.

La aplicacion Spring Boot se ejecuta con Java 21 y puede probarse localmente con el perfil `dev` y H2.
La suite automatizada actual termina correctamente con 185 pruebas, incluyendo pruebas de
integracion web. En esta revision se valido el arranque local con H2, el flujo real de pago
simulado del residente, el login y las restricciones de los tres roles, y el arranque `prod` en
Docker contra MySQL 8.4 con Flyway hasta V14; el envio SMTP externo aun requiere una cuenta de
prueba configurada en el entorno.
La validacion manual local confirmo registro y primer ingreso de RESIDENTE, cambio obligatorio de
contrasena, acceso operativo de PORTERIA, restricciones entre roles, formularios POST con CSRF y
navegacion de los modulos principales. Tambien se completo un pago PSE simulado de extremo a
extremo, desde la obligacion creada por ADMIN hasta la factura PDF. La integracion FastAPI y los
proveedores externos deben revalidarse en el entorno que se vaya a desplegar.
Se agrego una recuperacion administrativa controlada por variables de entorno, desactivada por
defecto, que obliga a cambiar la clave en el primer acceso y evita promover cuentas de otros roles.
El perfil `dev` crea un administrador local si no existe: `admin@nexur.com` con clave `Admin123!`.
En un entorno compartido se recomienda cambiarla usando `DEV_ADMIN_PASSWORD`.
Si la carga de datos de desarrollo falla, la aplicacion detiene el arranque y muestra la causa en lugar de continuar con cuentas incompletas.

## Avance estimado

Estimacion actual: **92% hacia una version candidata a despliegue**.

- Base tecnica, autenticacion y navegacion: 75%
- Roles y permisos: 95%
- Residentes, apartamentos y registro controlado: 65%
- Pagos y reservas: 95%
- Visitantes, porteria, parqueaderos, vehiculos e incidencias: 80%
- Reportes y exportaciones: 88%
- Pruebas automatizadas y pruebas por rol: 94%
- Operacion final, SMTP, migraciones, auditoria y respaldos: 78%

El porcentaje no mide cantidad de pantallas, sino cuanto falta para tener flujos completos, seguros, probados y desplegables.

## Correo real para recuperacion

El formulario de recuperacion envia un enlace real cuando se configuran estas variables antes de iniciar la aplicacion:

```powershell
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "tu-correo@gmail.com"
$env:MAIL_PASSWORD = "tu-contrasena-de-aplicacion"
$env:APP_BASE_URL = "http://localhost:8080"
$env:NOTIFICATIONS_EMAIL_ENABLED = "true"
```

Para Gmail se debe usar una contraseña de aplicacion con verificacion en dos pasos; nunca se debe guardar esa clave en Git ni pegarla en el codigo.
Si SMTP no esta configurado, la pantalla informa el problema en lugar de simular que el correo fue enviado.
Las notificaciones de PQRS por correo requieren ademas `NOTIFICATIONS_EMAIL_ENABLED=true`; si no se activa, siguen funcionando como notificaciones internas.

## Funcionalidades implementadas

- Autenticacion, registro y autorizacion por roles.
- Inicio de sesion tolerante a espacios y mayusculas en el correo, con actualizacion transaccional del usuario.
- Recuperacion de contrasena con token de un solo uso y vencimiento.
- Gestion de usuarios, residentes y apartamentos.
- Modelo relacional validado para residente-apartamento unico; la asociacion multiple vista en la otra rama queda pendiente de una migracion coordinada de pagos, reservas, visitantes y parqueaderos.
- Pagos, cartera, generacion de cuotas, filtros y exportacion Excel.
- Factura/comprobante PDF individual por pago, con fechas de emision, vencimiento y pago, disponible para registros historicos autorizados.
- Importacion masiva de residentes desde Excel con validaciones, control de duplicados y auditoria.
- Estado de cuenta PDF individual para residentes, sin exponer pagos de otros apartamentos.
- Referencia única para pagos en línea y trazabilidad local de resultado, transacción simulada y fecha de procesamiento.
- Sandbox local de pagos: PSE y tarjeta recorren un checkout de demostración sin cobro real; transferencia y efectivo requieren confirmación administrativa.
- El sandbox conserva los resultados aprobado/pendiente/rechazado/anulado/error y solo `APPROVED` cambia el pago a `PAGADO`.
- La regla de pago por rol y método se aplica en controlador, servicio y vista; no depende solo de ocultar botones.
- Reservas con validacion de cruces y aprobacion administrativa.
- Reservas protegidas contra apartamentos ajenos y transiciones administrativas duplicadas.
- Parqueaderos.
- Solicitudes de visitantes separadas de la operacion de porteria: el residente solicita y porteria aprueba, rechaza, registra entrada y registra salida.
- Porteria opera con una cuenta tecnica independiente: no tiene perfil de residente, apartamento, cartera, reservas ni funciones administrativas.
- Estados de parqueaderos protegidos para no desasignar vehiculos durante una edicion.
- Incidencias/PQRS con estados y respuesta administrativa.
- Conversación por incidencia con comentarios privados entre residente y administración.
- Evidencias privadas de PQRS en PDF, PNG o JPG, con límite de 5 MB y descarga autorizada.
- Notificaciones internas persistentes para cambios y comentarios de incidencias.
- Notificaciones por correo de PQRS listas para activar con SMTP, sin bloquear la persistencia si un correo falla.
- SMTP de Gmail soportado mediante variables fuera del repositorio; el envio real de recuperación debe revalidarse en el entorno actual.
- Bitacora persistente de mutaciones autorizadas, visible solo para administradores y sin datos sensibles.
- Avisos generales persistidos, con publicación administrativa y consulta por la comunidad.
- Perfil propio y cambio obligatorio de contraseña para cuentas nuevas, bloqueado globalmente hasta completarlo.
- Reportes de pagos, reservas y visitantes con filtros y exportacion PDF; FastAPI/ReportLab queda disponible como proveedor opcional sin base de datos propia.
- Dashboard con datos reales de pagos, reservas, visitantes e incidencias relacionadas e indicadores operativos calculados.
- Formularios POST protegidos sin campos CSRF duplicados y reportes AJAX con token en metaetiquetas.
- Registro con retencion de datos no sensibles cuando la validacion falla.
- Vehiculos propios del residente, control de movimientos y desactivacion segura de usuarios.

## Configuracion local

Desde la carpeta del proyecto (el perfil `dev` ya es el predeterminado):

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicacion queda disponible en `http://localhost:8080`.
No se deben iniciar dos instancias a la vez porque H2 mantiene bloqueado el archivo `data/nexurdb.mv.db`.

## Pendientes para una version final

- Mantener la contraseña de aplicación fuera del repositorio y rotarla si se comparte o deja de ser necesaria.
- Probar importacion Excel con filas validas, duplicadas, apartamentos inconsistentes y archivos sobre el limite.
- Repetir las pruebas funcionales con usuarios de cada rol contra el entorno MySQL de aceptación.
- Repetir el login funcional en `prod` despues de definir/resetear de forma controlada la clave de `admin@nexur.com`.
- Repetir antes de cada despliegue el respaldo y la migracion versionada de `MYSQL_MIGRATION.md`; la prueba local en `nexur_db` ya quedo en V14.
- Completar respaldos automatizados y politicas de proteccion de datos; la auditoria base ya esta implementada.
- Crear un usuario MySQL exclusivo para la aplicacion con permisos minimos; no usar `root` en `DB_USERNAME` fuera de esta validacion.
- Ejecutar una prueba de carga reproducible y revisar consultas del dashboard antes del despliegue.
- Validar en un entorno objetivo los limites de recursos, persistencia, proxy TLS y backups de Docker.
- Validar el codigo residencial mediante un proceso de entrega administrado y documentado.
- Repetir en aceptacion la prueba manual de registro, login, pagos, solicitudes de visitantes y permisos por rol.
- Ejecutar manualmente los cinco resultados del sandbox local; la suite automatizada ya cubre que solo `APPROVED` cambia el pago a `PAGADO`.
- Validar el envio SMTP real de recuperación, PQRS y notificaciones en el ambiente de aceptación.
- Completar las historias de usuario HU-006 en adelante y mantener la matriz de [REQUIREMENTS_TRACEABILITY.md](REQUIREMENTS_TRACEABILITY.md).

## Resultado de la revision del 03/09/2026

- `.\mvnw.cmd clean test`: 185 pruebas, 0 fallos y 0 errores.
- JaCoCo: paquete `service` con `90,2%` de lineas y `72,3%` de ramas.
- Arranque `dev` con H2: correcto en `http://localhost:8080`.
- Login real de `admin@nexur.com`: correcto y redirige al dashboard.
- Smoke HTTP de ADMIN, RESIDENTE y PORTERIA: rutas principales en `200`, restricciones de rol en `403` y formularios POST con token CSRF.
- Flujo de pago simulado: creacion administrativa, inicio PSE, checkout local `APPROVED`, conciliacion y factura PDF en `200 application/pdf`.
- Correcciones aplicadas: el inicializador de desarrollo ya no reemplaza contrasenas
  existentes en cada reinicio; la aprobacion/rechazo de reservas ya no termina en HTTP 500;
  la validacion de formularios se enlaza despues de cargar el DOM; los formularios POST incluyen
  CSRF; los enlaces internos usan rutas Thymeleaf; los reportes vuelven al generador local si
  FastAPI esta apagado o devuelve un PDF vacio.
- Validaciones operativas restantes: SMTP real, aceptación con datos controlados,
  pruebas visuales por rol, carga/estres y respaldo/restauracion en el servidor objetivo.
