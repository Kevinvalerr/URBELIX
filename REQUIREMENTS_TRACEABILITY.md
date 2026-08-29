# URBELIX - Trazabilidad de requisitos

Estado basado en la documentacion existente, pero contrastado con el codigo y las pruebas
actuales de `URBELIXXX` el 29/08/2026. La documentacion de referencia se considera desactualizada
cuando contradice el comportamiento comprobado.

Suite actual: 113 pruebas automatizadas, incluyendo 40 de integracion web con H2.

## Requisitos funcionales

| ID | Estado | Evidencia / pendiente |
| --- | --- | --- |
| RF-01 | Cumple ampliado | Registro e importacion Excel con validacion, correo unico, apartamento y codigo residencial. El registro controlado fue validado manualmente en H2; falta repetirlo con datos del entorno de aceptacion. |
| RF-02 | Cumple | Login por correo y contrasena con Spring Security; el correo se normaliza para evitar fallos por espacios o mayusculas. |
| RF-03 | Cumple ampliado | Token de un solo uso y vencimiento implementados; el envio SMTP real debe revalidarse en el entorno actual. |
| RF-04 | Cumple ampliado | ADMIN, RESIDENTE y PORTERIA definidos. |
| RF-05 | Cumple | Restricciones por ruta y `@PreAuthorize`, con pantalla de acceso denegado. |
| RF-06 | Cumple | Administrador genera cuotas de administracion por residente. |
| RF-07 | Cumple ampliado | Dashboard y cartera calculan estados; el residente puede descargar su estado de cuenta PDF con pagos, vencimientos, fechas efectivas y saldo pendiente. |
| RF-08 | Cumple ampliado | Administrador registra pagos y los asocia a residente/apartamento; transferencia y efectivo requieren confirmación administrativa. |
| RF-09 | Cumple ampliado | Residente consulta solo sus pagos y estados; en `dev` puede recorrer un checkout local de PSE o tarjeta sin cobro. Se verifico manualmente el flujo PSE hasta `APPROVED` y factura PDF. |
| RF-10 | Cumple ampliado | Pagos persistidos y listados como historial; cada registro visible permite descargar su factura/comprobante PDF individual. |
| RF-11 | Cumple | Administrador publica, consulta y desactiva avisos generales persistidos. |
| RF-12 | Cumple | Residente y portería consultan solo avisos vigentes. |
| RF-13 | Cumple | Residente solicita reservas de zonas comunes. |
| RF-14 | Cumple | Se validan rangos futuros y cruces de horario. |
| RF-15 | Cumple | Administrador aprueba o rechaza reservas. |
| RF-16 | Cumple | Administrador crea, edita y elimina parqueaderos con validaciones. |
| RF-17 | Cumple ampliado | Residente registra y edita sus propios vehiculos; solo PORTERIA gestiona el catalogo y los movimientos operativos. |
| RF-18 | Cumple | Asignacion de parqueadero a residente/vehiculo con validacion de tipo y apartamento. |
| RF-19 | Cumple parcial alto | Residente crea incidencias, agrega comentarios, adjunta evidencias privadas y consulta respuestas; administrador gestiona estados, comentarios y evidencias; las notificaciones internas y el envio por correo opcional ya estan implementados. Falta validar el correo real de PQRS y el proveedor externo de pagos. |

## Requisitos no funcionales

| ID | Estado | Evidencia / pendiente |
| --- | --- | --- |
| RNF-01 | Cumple | Contrasenas con BCrypt; nunca se guardan en texto plano. |
| RNF-02 | Cumple | Control por roles en rutas y metodos; el primer ingreso queda bloqueado globalmente hasta cambiar la contraseña temporal. |
| RNF-03 | No verificado | No existe medicion reproducible de respuesta menor a 3 segundos bajo carga normal. |
| RNF-04 | No verificado | No existe prueba de 100 usuarios concurrentes. |
| RNF-05 | Parcial | Plantillas Bootstrap y CSS responsivo; las vistas principales entregan correctamente en local, pero falta verificacion visual en navegadores y dispositivos definidos. |
| RNF-06 | Parcial | Navegacion y mensajes mejorados; falta prueba de usabilidad con usuarios finales. |
| RNF-07 | Parcial alto | Entidades tienen claves foraneas y restricciones; V1-V12 estan disponibles y la importacion Excel valida relaciones con apartamento y codigo residencial. Falta repetir el arranque `prod` y las migraciones sobre el MySQL final. |
| RNF-08 | Cumple | Separacion Model, Repository, Service y Controller visible en el proyecto. |
| RNF-09 | Cumple | Repositorio Git disponible. Falta revisar politica de ramas y entregas. |
| RNF-10 | Parcial alto | Modulos separados, consultas del dashboard separadas por rol, auditoria persistente y migraciones versionadas; falta desacoplar avisos y notificaciones. |

## Brechas del DEA y de las historias

- El DEA menciona cambio obligatorio de contrasena en el primer ingreso; esta implementado para registro y cuentas nuevas, con redireccion al perfil.
- El DEA menciona gestion de perfil; existe un flujo propio para actualizar nombre y telefono sin modificar rol, correo o apartamento.
- PSE y tarjeta ya tienen referencia única, checkout local de prueba, webhook con firma, validación de monto e idempotencia; falta conectar y validar el proveedor real. Transferencia y efectivo siguen el circuito de validación administrativa. La factura individual se genera bajo autorización y conserva el historial.
- Visitantes tienen flujo por estado: RESIDENTE solicita; PORTERIA aprueba, rechaza, registra entrada y registra salida. ADMIN no accede a la operación de portería.
- El archivo de historias contiene HU-001 a HU-005, aunque los requisitos funcionales abarcan RF-01 a RF-19. Faltan historias y criterios para reservas, visitantes, parqueaderos, vehiculos, incidencias y reportes.
- El DEA menciona un microservicio FastAPI de reportes; ya esta incluido como proveedor opcional y no persiste datos de negocio. Spring Boot mantiene el PDF local como respaldo.

## Brechas que impiden declarar version final

- No existen aun `Dockerfile` ni `docker-compose.yml` verificados.
- No hay mediciones reproducibles para RNF-03 ni prueba de 100 usuarios concurrentes para RNF-04.
- Faltan pruebas de navegador de los tres roles y una repeticion controlada contra MySQL/Flyway.
- El smoke HTTP de los tres roles y el flujo simulado de pagos ya fueron verificados en H2; falta repetirlos con navegador y MySQL/Flyway.
- El correo SMTP, Wompi/PSE externos, backups y restauracion requieren validacion operativa.
