# Especificacion de requisitos de URBELIX

## 1. Control del documento

| Campo | Valor |
| --- | --- |
| Producto | URBELIX - sistema web de gestion residencial |
| Carpeta de trabajo | `URBELIXXX` |
| Version de referencia | Estado de la rama `develop` |
| Fecha de revision | 2026-09-03 |
| Plataforma objetivo | Aplicacion web para navegador de escritorio |
| Persistencia de desarrollo | H2 en archivo |
| Persistencia de produccion | MySQL con Flyway |
| Idioma de interfaz | Espanol |
| Moneda de negocio | COP |

Este documento es la fuente de verdad para planear y ejecutar las pruebas. Solo se
considera cumplido un requisito cuando existe implementacion y una prueba o
verificacion identificable.

### Estados

- `IMPLEMENTADO`: existe en el codigo y tiene cobertura automatizada o verificacion local.
- `PARCIAL`: existe una parte, pero falta validar un entorno, un caso o un criterio.
- `PENDIENTE`: es necesario para la version final, pero aun no esta implementado o medido.
- `FUERA DE ALCANCE ACTUAL`: no se exige para esta entrega, aunque podria agregarse despues.

## 2. Alcance

URBELIX permite administrar usuarios, apartamentos, residentes, cartera, pagos
simulados, reservas, visitantes, porteria, parqueaderos, vehiculos, incidencias,
avisos, notificaciones, reportes, exportaciones y auditoria.

El pago PSE/tarjeta es un sandbox local. No representa un cobro real y no debe
presentarse como integracion productiva mientras no se configure y valide un
proveedor externo. FastAPI es un proveedor opcional para PDF; Spring Boot tiene
un generador local de respaldo.

## 3. Actores y reglas de rol

| Actor | Responsabilidad | Puede hacer | No puede hacer |
| --- | --- | --- | --- |
| Visitante no autenticado | Consultar acceso publico y autenticarse | Ver inicio, login, registro y recuperacion | Consultar informacion privada |
| ADMIN | Administrar la copropiedad | Gestionar usuarios, residentes, apartamentos, cartera, reservas, incidencias, avisos, reportes, auditoria y catalogo de parqueaderos | Operar el control de porteria como portero |
| RESIDENTE | Gestionar su vida residencial | Consultar sus pagos, pagar en sandbox, descargar comprobantes, reservar su apartamento, solicitar visitantes, gestionar sus vehiculos, crear y consultar sus incidencias y actualizar su perfil | Ver datos de otros residentes, aprobar visitantes, registrar entradas/salidas, administrar usuarios o catalogos |
| PORTERIA | Operar acceso y parqueadero | Revisar solicitudes, aprobar/rechazar visitantes, registrar entradas/salidas y operar movimientos de parqueadero | Tener apartamento o residente asociado, consultar cartera/reservas, administrar usuarios o parqueaderos |

Reglas transversales de autorizacion:

- El rol se asigna con `ADMIN`, `RESIDENTE` o `PORTERIA`.
- Una cuenta `PORTERIA` no crea ni mantiene una entidad residente.
- Las rutas administrativas se protegen en configuracion y, cuando aplica, en el controlador.
- Un residente solo opera registros propios o relacionados con su apartamento.
- El usuario autenticado puede cambiar su nombre, telefono y contrasena, pero no su rol,
  correo ni apartamento desde el perfil.
- Las operaciones de cambio de estado se realizan mediante formularios `POST` protegidos.

## 4. Requisitos funcionales

### 4.1 Acceso, cuentas y seguridad de identidad

| ID | Requisito verificable | Estado | Prueba o evidencia |
| --- | --- | --- | --- |
| RF-AUT-01 | El sistema debe mostrar una pagina publica de inicio y permitir ir a login, registro y recuperacion. | IMPLEMENTADO | `AuthController`, vistas publicas |
| RF-AUT-02 | El sistema debe autenticar por correo y contrasena mediante Spring Security. | IMPLEMENTADO | `NexurIntegrationTest`, `UsuarioDetailsServiceTest` |
| RF-AUT-03 | El correo de login debe normalizar espacios y mayusculas sin crear identidades duplicadas. | IMPLEMENTADO | `UsuarioDetailsServiceTest`, `UsuarioServiceTest` |
| RF-AUT-04 | Las credenciales invalidas deben devolver un mensaje claro sin revelar que dato fallo. | IMPLEMENTADO | `AuthController`, vista de login |
| RF-AUT-05 | Cerrar sesion debe invalidar la sesion y eliminar la cookie `JSESSIONID`. | IMPLEMENTADO | Configuracion de logout |
| RF-AUT-06 | El registro publico debe crear exclusivamente cuentas `RESIDENTE`. | IMPLEMENTADO | `AuthController`, `UsuarioService` |
| RF-AUT-07 | El registro debe exigir nombre, correo, contrasena, confirmacion, documento, telefono, apartamento y codigo de registro. | IMPLEMENTADO | Validaciones de formulario y servicio |
| RF-AUT-08 | El codigo de registro debe coincidir con el codigo del apartamento seleccionado. | IMPLEMENTADO | `UsuarioServiceTest`, `NexurIntegrationTest` |
| RF-AUT-09 | El registro debe rechazar correo y documento duplicados, apartamento inexistente y datos con formato invalido. | IMPLEMENTADO | `UsuarioServiceTest`, validaciones Bean Validation |
| RF-AUT-10 | Una cuenta creada por registro o por ADMIN debe marcarse para cambio de contrasena inicial. | IMPLEMENTADO | `FirstLoginPasswordFilter`, servicios de usuario y smoke local |
| RF-AUT-11 | El primer ingreso debe redirigir al perfil y bloquear el resto de la aplicacion hasta cambiar la contrasena. | IMPLEMENTADO | `FirstLoginPasswordFilter`, smoke local |
| RF-AUT-12 | La nueva contrasena debe cumplir longitud y contener mayuscula, minuscula, numero y caracter especial. | IMPLEMENTADO | `UsuarioServiceTest` |
| RF-AUT-13 | El usuario debe poder solicitar recuperacion de contrasena mediante su correo. | PARCIAL | Servicio y token implementados; SMTP real debe validarse |
| RF-AUT-14 | El enlace de recuperacion debe usar un token de un solo uso con expiracion. | IMPLEMENTADO | `PasswordResetServiceTest` |
| RF-AUT-15 | El sistema no debe guardar contrasenas en texto plano. | IMPLEMENTADO | BCrypt en `SecurityConfig` y servicio |
| RF-AUT-16 | El usuario debe poder actualizar su nombre y telefono desde su perfil. | IMPLEMENTADO | `PerfilController`, `UsuarioServiceTest` |
| RF-AUT-17 | El ADMIN debe poder habilitar o deshabilitar cuentas sin poder deshabilitarse a si mismo ni dejar cero administradores activos. | IMPLEMENTADO | `UsuarioServiceTest` |
| RF-AUT-18 | El entorno productivo debe permitir recuperar de forma controlada una cuenta ADMIN solo mediante variables seguras. | PARCIAL | `AdminBootstrapRunner`; falta prueba operacional en prod |

### 4.2 Usuarios, residentes y apartamentos

| ID | Requisito verificable | Estado | Prueba o evidencia |
| --- | --- | --- | --- |
| RF-ADM-01 | ADMIN debe listar los usuarios con rol, estado, correo y apartamento cuando aplique. | IMPLEMENTADO | `UsuarioController`, vista de usuarios |
| RF-ADM-02 | ADMIN debe crear usuarios `RESIDENTE` y `PORTERIA`. | IMPLEMENTADO | `UsuarioServiceTest` |
| RF-ADM-03 | Crear un residente debe exigir documento, telefono y apartamento existente. | IMPLEMENTADO | `UsuarioServiceTest` |
| RF-ADM-04 | Crear un usuario `PORTERIA` no debe crear residente, apartamento, cartera ni reservas asociadas. | IMPLEMENTADO | `UsuarioServiceTest`, modelo `Usuario` |
| RF-ADM-05 | ADMIN debe editar datos permitidos y mantener las relaciones consistentes con el rol. | PARCIAL | Servicio implementado; faltan pruebas de todos los cambios de rol |
| RF-ADM-06 | ADMIN debe listar, crear, editar y eliminar apartamentos. | IMPLEMENTADO | `ApartamentoController`, servicio |
| RF-ADM-07 | Un apartamento nuevo debe recibir un codigo de registro si no se proporciona. | IMPLEMENTADO | `ApartamentoService` |
| RF-ADM-08 | No se debe eliminar un apartamento con pagos o reservas asociados. | IMPLEMENTADO | `ApartamentoService` |
| RF-ADM-09 | ADMIN debe importar apartamentos desde Excel e ignorar filas vacias o duplicadas. | PARCIAL | Implementado; falta prueba dedicada de archivo |
| RF-ADM-10 | ADMIN debe crear, editar y eliminar residentes, validando documento y apartamento. | IMPLEMENTADO | `ResidenteServiceTest` |
| RF-ADM-11 | ADMIN debe importar residentes desde Excel con validacion fila a fila y reporte de creados, duplicados y errores. | PARCIAL | `ExcelController`, modelos de importacion; falta prueba completa de archivo |
| RF-ADM-12 | ADMIN debe descargar plantilla y exportar residentes a Excel. | IMPLEMENTADO | `ExcelController`, `ExcelExportService` |
| RF-ADM-13 | La asociacion residente-apartamento debe mantenerse consistente con pagos, reservas, visitantes y vehiculos. | PARCIAL | Validaciones principales; falta validar todos los flujos en MySQL |

### 4.3 Dashboard y navegacion

| ID | Requisito verificable | Estado | Prueba o evidencia |
| --- | --- | --- | --- |
| RF-DAS-01 | El sistema debe mostrar un dashboard despues de iniciar sesion. | IMPLEMENTADO | Smoke HTTP por roles |
| RF-DAS-02 | El dashboard de ADMIN debe mostrar indicadores globales de apartamentos, residentes, pagos, mora, multas, reservas e incidencias. | IMPLEMENTADO | `DashboardController` |
| RF-DAS-03 | El dashboard de RESIDENTE debe mostrar apartamento, pagos propios, reservas propias, visitantes activos, mora y notificaciones. | IMPLEMENTADO | `DashboardController` |
| RF-DAS-04 | El dashboard de PORTERIA debe dirigir a la operacion de visitantes y parqueaderos sin informacion administrativa. | IMPLEMENTADO | `PorteriaController`, vista de porteria |
| RF-DAS-05 | La navegacion debe ocultar opciones que el rol no puede usar y las rutas deben rechazar acceso directo no autorizado. | IMPLEMENTADO | `SecurityConfig`, smoke de 403 |
| RF-DAS-06 | Un acceso no autorizado debe mostrar una pagina de acceso denegado utilizable. | IMPLEMENTADO | `ErrorController`, vista de error |

### 4.4 Pagos, cartera y comprobantes

| ID | Requisito verificable | Estado | Prueba o evidencia |
| --- | --- | --- | --- |
| RF-PAG-01 | ADMIN debe registrar pagos con residente, apartamento, monto, tipo, metodo, fecha y vencimiento. | IMPLEMENTADO | `PagoController`, `PagoServiceTest` |
| RF-PAG-02 | El apartamento del pago debe pertenecer al residente seleccionado. | IMPLEMENTADO | `PagoServiceTest` |
| RF-PAG-03 | ADMIN debe generar la cuota mensual de administracion una sola vez por residente y periodo. | IMPLEMENTADO | `PagoService`, prueba de servicio |
| RF-PAG-04 | El sistema debe manejar estados `PENDIENTE`, `PAGADO` y `VENCIDO` segun vencimiento y fecha actual. | IMPLEMENTADO | `PagoService` |
| RF-PAG-05 | ADMIN debe confirmar pagos de efectivo o transferencia y registrar la fecha de pago. | IMPLEMENTADO | `PagoController`, `PagoServiceTest` |
| RF-PAG-06 | Pagos PSE y tarjeta no deben marcarse manualmente como pagados por ADMIN. | IMPLEMENTADO | `PagoServiceTest` |
| RF-PAG-07 | RESIDENTE debe consultar unicamente sus pagos y sus totales. | IMPLEMENTADO | `PagoController`, smoke por rol |
| RF-PAG-08 | RESIDENTE debe iniciar el checkout solo para sus pagos pendientes o vencidos configurados como PSE o tarjeta. | IMPLEMENTADO | `PagoServiceTest`, flujo local |
| RF-PAG-09 | El checkout local debe generar una referencia unica por pago. | IMPLEMENTADO | `PagoServiceTest`, `PagoSimulacionServiceTest` |
| RF-PAG-10 | El sandbox debe permitir simular aprobado, pendiente, rechazado, anulado y error sin cobrar dinero real. | IMPLEMENTADO | `PagoSimulacionServiceTest`, flujo PSE local |
| RF-PAG-11 | Un resultado aprobado del sandbox local debe registrar estado, transaccion simulada y fecha de pago. | IMPLEMENTADO | `PagoServiceTest`, `PagoSimulacionServiceTest` |
| RF-PAG-12 | El sistema debe validar resultado, propietario, metodo y estado antes de registrar una simulacion; no debe existir cobro externo. | IMPLEMENTADO | `PagoServiceTest`, `PagoSimulacionServiceTest` |
| RF-PAG-13 | ADMIN y RESIDENTE autorizado deben descargar una factura PDF individual de cualquier pago visible para su rol. | IMPLEMENTADO | `FacturaPagoPdfServiceTest`, smoke PDF |
| RF-PAG-14 | RESIDENTE debe descargar su estado de cuenta PDF sin incluir pagos de otros apartamentos. | IMPLEMENTADO | `EstadoCuentaPdfServiceTest` |
| RF-PAG-15 | ADMIN debe exportar pagos a Excel. | IMPLEMENTADO | `ExcelExportService`, ruta `/pagos/excel` |
| RF-PAG-16 | La pantalla de pagos debe filtrar por varios criterios combinables: estado, tipo, metodo, residente, apartamento y rango de fechas. | IMPLEMENTADO | `PagoController`, `PagoService`, vista de pagos y exportacion Excel |
| RF-PAG-17 | Las facturas historicas deben conservar fecha de emision, vencimiento, pago, referencia, metodo y estado. | PARCIAL | Modelo y PDF implementados; falta validar casos historicos en MySQL |
| RF-PAG-18 | El sistema debe impedir doble confirmacion o cambios de estado invalidos. | IMPLEMENTADO | Reglas de `PagoService` |

### 4.5 Reservas

| ID | Requisito verificable | Estado | Prueba o evidencia |
| --- | --- | --- | --- |
| RF-RES-01 | RESIDENTE debe consultar sus reservas y ADMIN todas las reservas. | IMPLEMENTADO | `PagoReservaController` |
| RF-RES-02 | RESIDENTE debe crear reservas de BBQ, piscina, gimnasio o salon social usando su apartamento. | IMPLEMENTADO | `ReservaServiceTest` |
| RF-RES-03 | La reserva debe tener inicio y fin validos, con inicio futuro y fin posterior al inicio. | IMPLEMENTADO | `ReservaServiceTest` |
| RF-RES-04 | El sistema debe impedir cruces de horario para el mismo espacio en reservas pendientes o aprobadas. | IMPLEMENTADO | `ReservaServiceTest` |
| RF-RES-05 | ADMIN debe aprobar o rechazar reservas pendientes con observacion opcional. | IMPLEMENTADO | `PagoReservaController`, servicio |
| RF-RES-06 | No se deben aprobar ni rechazar dos veces reservas que ya cambiaron de estado. | IMPLEMENTADO | `ReservaServiceTest` |
| RF-RES-07 | El residente no debe reservar un apartamento diferente al suyo. | IMPLEMENTADO | `NexurIntegrationTest`, `ReservaServiceTest` |
| RF-RES-08 | La vista debe mostrar estado, fechas, espacio, apartamento y observaciones. | IMPLEMENTADO | Vista de reservas |

### 4.6 Visitantes y operacion de porteria

| ID | Requisito verificable | Estado | Prueba o evidencia |
| --- | --- | --- | --- |
| RF-VIS-01 | RESIDENTE debe crear una solicitud de visitante asociada automaticamente a su apartamento. | IMPLEMENTADO | `VisitanteServiceTest`, smoke |
| RF-VIS-02 | La solicitud debe exigir nombre y documento numerico valido. | IMPLEMENTADO | Modelo y servicio |
| RF-VIS-03 | RESIDENTE debe consultar unicamente solicitudes de su apartamento. | IMPLEMENTADO | `VisitanteController` |
| RF-VIS-04 | PORTERIA debe consultar las solicitudes y visitantes operativos de toda la copropiedad. | IMPLEMENTADO | `VisitanteController` |
| RF-VIS-05 | PORTERIA debe aprobar o rechazar una solicitud pendiente. | IMPLEMENTADO | `VisitanteServiceTest` |
| RF-VIS-06 | Un rechazo debe permitir registrar un motivo limitado a 500 caracteres. | IMPLEMENTADO | `VisitanteServiceTest` |
| RF-VIS-07 | PORTERIA debe registrar entrada solo para visitantes aprobados y salida solo para visitantes dentro. | IMPLEMENTADO | `VisitanteServiceTest` |
| RF-VIS-08 | El ciclo de estados debe ser `PENDIENTE -> APROBADA/RECHAZADA -> DENTRO -> FINALIZADA`. | IMPLEMENTADO | `EstadoVisitante`, servicio |
| RF-VIS-09 | ADMIN no debe operar las rutas de control de visitantes. | IMPLEMENTADO | `SecurityConfig`, smoke de 403 |

### 4.7 Parqueaderos, vehiculos y movimientos

| ID | Requisito verificable | Estado | Prueba o evidencia |
| --- | --- | --- | --- |
| RF-PAR-01 | ADMIN debe crear, editar y eliminar espacios de parqueadero. | IMPLEMENTADO | `ParqueaderoServiceTest` |
| RF-PAR-02 | Cada espacio debe tener numero, tipo de vehiculo y estado valido. | IMPLEMENTADO | Modelo y servicio |
| RF-PAR-03 | Un espacio asignado u ocupado debe mantener sus relaciones de apartamento y vehiculo coherentes. | IMPLEMENTADO | `ParqueaderoServiceTest` |
| RF-PAR-04 | ADMIN no debe eliminar un espacio con vehiculo o movimientos historicos asociados. | IMPLEMENTADO | `ParqueaderoServiceTest` |
| RF-PAR-05 | RESIDENTE debe consultar y registrar sus propios vehiculos. | IMPLEMENTADO | `VehiculoServiceTest` |
| RF-PAR-06 | La placa debe tener formato valido y no duplicarse. | IMPLEMENTADO | `VehiculoServiceTest` |
| RF-PAR-07 | RESIDENTE no debe editar un vehiculo que pertenece a otro residente. | IMPLEMENTADO | `VehiculoServiceTest` |
| RF-PAR-08 | PORTERIA debe gestionar el catalogo operativo de vehiculos y movimientos. | IMPLEMENTADO | `VehiculoController`, `MovimientoParqueaderoController` |
| RF-PAR-09 | PORTERIA debe registrar ingreso validando espacio, tipo, apartamento, asignacion y duplicidad de ingreso activo. | IMPLEMENTADO | `ParqueaderoServiceTest`, `MovimientoParqueaderoService` |
| RF-PAR-10 | PORTERIA debe registrar salida, liberar espacio no asignado y conservar asignado el espacio residencial. | IMPLEMENTADO | `ParqueaderoServiceTest` |
| RF-PAR-11 | PORTERIA debe consultar historial filtrado por placa, tipo, espacio, estado y fecha. | IMPLEMENTADO | `MovimientoParqueaderoService` |
| RF-PAR-12 | RESIDENTE no debe acceder a las rutas operativas de porteria ni administrar el catalogo global. | IMPLEMENTADO | `SecurityConfig`, smoke de 403 |

### 4.8 Incidencias, PQRS, evidencias y notificaciones

| ID | Requisito verificable | Estado | Prueba o evidencia |
| --- | --- | --- | --- |
| RF-INC-01 | RESIDENTE debe crear una incidencia con asunto, descripcion y tipo. | IMPLEMENTADO | `IncidenciaServiceTest`, smoke |
| RF-INC-02 | RESIDENTE debe consultar unicamente sus incidencias y su historial. | IMPLEMENTADO | `IncidenciaController`, servicio |
| RF-INC-03 | ADMIN debe consultar todas las incidencias. | IMPLEMENTADO | `IncidenciaController` |
| RF-INC-04 | ADMIN debe cambiar el estado entre abierto, en revision, resuelto y cerrado. | IMPLEMENTADO | `IncidenciaServiceTest` |
| RF-INC-05 | ADMIN debe registrar respuesta u observacion al actualizar una incidencia. | IMPLEMENTADO | Servicio y vista |
| RF-INC-06 | ADMIN y RESIDENTE autorizado deben agregar comentarios a la incidencia. | IMPLEMENTADO | `IncidenciaServiceTest` |
| RF-INC-07 | ADMIN y RESIDENTE autorizado deben adjuntar evidencias PDF, PNG o JPG de hasta 5 MB. | IMPLEMENTADO | `ArchivoStorageServiceTest`, servicio |
| RF-INC-08 | Las evidencias deben guardarse fuera de recursos publicos y descargarse solo con autorizacion. | IMPLEMENTADO | `ArchivoStorageService`, controlador |
| RF-INC-09 | El sistema debe crear notificaciones internas para eventos relevantes y permitir marcarlas como leidas. | IMPLEMENTADO | `NotificacionServiceTest` |
| RF-INC-10 | El sistema debe poder enviar notificaciones de PQRS por SMTP sin perder la persistencia si el correo falla. | PARCIAL | Servicio implementado; SMTP real pendiente de validacion |

### 4.9 Avisos, reportes, exportaciones y auditoria

| ID | Requisito verificable | Estado | Prueba o evidencia |
| --- | --- | --- | --- |
| RF-REP-01 | ADMIN debe crear, publicar, consultar y desactivar avisos generales. | IMPLEMENTADO | `AvisoService`, controlador |
| RF-REP-02 | RESIDENTE y PORTERIA deben consultar solo avisos activos y vigentes. | IMPLEMENTADO | `AvisoService` |
| RF-REP-03 | ADMIN debe consultar reportes de pagos, reservas, visitantes o todos. | IMPLEMENTADO | `ReporteController`, `ReporteServiceTest` |
| RF-REP-04 | Los reportes deben aceptar tipo y rango de fechas y rechazar rangos invalidos. | IMPLEMENTADO | `ReporteServiceTest`, `ReporteControllerTest` |
| RF-REP-05 | ADMIN debe descargar reportes en PDF. | IMPLEMENTADO | Generador local y fallback FastAPI |
| RF-REP-06 | FastAPI/ReportLab debe poder actuar como proveedor opcional sin ser requisito para generar PDF local. | IMPLEMENTADO | `ReportesFastApiService`, prueba de fallback |
| RF-REP-07 | ADMIN debe consultar una bitacora de mutaciones con actor, accion, entidad, fecha y detalle no sensible. | IMPLEMENTADO | `AuditoriaRequestFilter`, `AuditoriaController` |
| RF-REP-08 | Los reportes y exportaciones no deben exponer datos a roles no autorizados. | IMPLEMENTADO | Seguridad de rutas |
| RF-REP-09 | Los reportes de pagos deben tener filtrado multicriterio reutilizable con la pantalla de pagos. | IMPLEMENTADO | `ReporteController`, `ReporteService`, vista de reportes y exportacion Excel |

## 5. Requisitos no funcionales

| ID | Requisito medible o verificable | Estado | Como se verifica |
| --- | --- | --- | --- |
| RNF-SEG-01 | Las contrasenas deben almacenarse con BCrypt y nunca en texto plano. | IMPLEMENTADO | Inspeccion de servicio y pruebas |
| RNF-SEG-02 | Todas las rutas privadas deben requerir autenticacion. | IMPLEMENTADO | `SecurityConfig`, pruebas de integracion |
| RNF-SEG-03 | La autorizacion debe aplicarse por rol en rutas y metodos, no solo ocultando botones. | IMPLEMENTADO | Pruebas 200/403 por rol |
| RNF-SEG-04 | Las operaciones POST deben usar proteccion CSRF. | IMPLEMENTADO | Auditoria de formularios y configuracion |
| RNF-SEG-05 | El sandbox local debe rechazar resultados invalidos, pagos ajenos, metodos no simulables y estados no cobrables. | IMPLEMENTADO | `PagoSimulacionServiceTest` |
| RNF-SEG-06 | Los archivos subidos deben validar extension/tipo, tamano y ruta de almacenamiento. | IMPLEMENTADO | `ArchivoStorageServiceTest`; revisar pruebas de contenido malicioso |
| RNF-SEG-07 | Los tokens de recuperacion deben expirar y quedar inutilizables despues de usarse. | IMPLEMENTADO | `PasswordResetServiceTest` |
| RNF-SEG-08 | Variables de SMTP, MySQL y proveedores externos no deben guardarse en Git. | IMPLEMENTADO | Configuracion por variables; escaneo del repositorio |
| RNF-SEG-09 | El sistema debe evitar enumerar usuarios al solicitar recuperacion de contrasena. | IMPLEMENTADO | Mensaje generico del flujo |
| RNF-DAT-01 | Las entidades deben mantener claves foraneas y estados validos. | PARCIAL | Migraciones V0.1-V14; falta validacion completa contra MySQL de aceptacion |
| RNF-DAT-02 | Flyway debe controlar la evolucion del esquema productivo y fallar si una migracion es invalida. | PARCIAL | Configuracion prod; falta ejecucion repetible en entorno objetivo |
| RNF-DAT-03 | Las transiciones de pagos, reservas, visitantes e ingresos deben ser consistentes ante reintentos. | IMPLEMENTADO | Pruebas de servicios; falta prueba concurrente |
| RNF-DAT-04 | La auditoria debe conservar trazabilidad de mutaciones autorizadas sin registrar contrasenas ni secretos. | IMPLEMENTADO | Filtro y entidad `Auditoria` |
| RNF-DAT-05 | Los comprobantes y estados de cuenta deben generarse desde datos persistidos y respetar autorizacion. | IMPLEMENTADO | Pruebas PDF y control de acceso |
| RNF-REN-01 | En uso normal local, las pantallas principales deberian responder en menos de 3 segundos. | PENDIENTE | Falta medicion automatizada y criterio de ambiente |
| RNF-REN-02 | El sistema deberia soportar al menos 100 usuarios concurrentes en el entorno de aceptacion sin errores criticos. | PENDIENTE | Falta prueba de carga/estres y umbrales acordados |
| RNF-REN-03 | Las consultas del dashboard y listados no deben cargar datos sin limite cuando el volumen lo vuelva costoso. | PENDIENTE | Falta paginacion y medicion de consultas |
| RNF-DIS-01 | La interfaz debe funcionar en navegadores de escritorio actuales y conservar una estructura visual consistente. | PARCIAL | Smoke local; falta matriz de navegadores y prueba visual |
| RNF-DIS-02 | La interfaz debe mostrar mensajes de validacion, exito, error y acceso denegado comprensibles. | IMPLEMENTADO | Vistas y validaciones; falta prueba de usabilidad |
| RNF-DIS-03 | Los formularios deben conservar datos no sensibles cuando una validacion falla. | IMPLEMENTADO | Registro y formularios principales |
| RNF-DIS-04 | La interfaz debe priorizar escritorio web; la compatibilidad movil no es criterio de aceptacion de esta entrega. | IMPLEMENTADO | Alcance acordado |
| RNF-DIS-05 | Las tablas, filtros y acciones no deben cortarse en el ancho objetivo de escritorio. | PARCIAL | CSS ajustado; falta verificacion visual con navegador |
| RNF-ARQ-01 | El codigo debe conservar separacion Controller, Service, Repository, Model y vistas Thymeleaf. | IMPLEMENTADO | Estructura del proyecto |
| RNF-ARQ-02 | La aplicacion debe iniciar con H2 en perfil `dev` sin depender de MySQL, SMTP ni FastAPI. | IMPLEMENTADO | `application-dev.properties`, suite local |
| RNF-ARQ-03 | El perfil `prod` debe usar MySQL, migraciones Flyway y datos iniciales controlados. | PARCIAL | Configurado; falta prueba en MySQL objetivo |
| RNF-ARQ-04 | El proveedor FastAPI debe ser opcional y el PDF local debe funcionar si esta caido. | IMPLEMENTADO | `ReporteControllerTest` |
| RNF-ARQ-05 | La configuracion de secretos debe estar fuera del codigo y poder cambiarse por ambiente. | IMPLEMENTADO | `application.properties` |
| RNF-ARQ-06 | Los errores esperados deben convertirse en mensajes de usuario o respuestas HTTP adecuadas, sin stack trace en la vista. | PARCIAL | Controladores principales; falta auditoria global de excepciones |
| RNF-OPS-01 | Debe existir una guia reproducible para levantar, probar y migrar el sistema. | IMPLEMENTADO | `README.md`, `TESTING_AND_DEPLOYMENT.md`, `MYSQL_MIGRATION.md` |
| RNF-OPS-02 | Debe existir una estrategia de respaldo y restauracion probada para MySQL. | PENDIENTE | Falta procedimiento automatizado y prueba de restauracion |
| RNF-OPS-03 | Debe existir `Dockerfile`, composicion de servicios y healthchecks verificables. | IMPLEMENTADO | `Dockerfile`, `docker-compose.yml`, `docker-compose.prod.yml` y smoke Docker |
| RNF-OPS-04 | El arranque debe fallar de forma explicita ante configuracion esencial invalida. | PARCIAL | Inicializacion controlada; falta matriz de fallos por ambiente |
| RNF-OPS-05 | Cada entrega debe quedar identificada por rama, commit y resultado de pruebas. | IMPLEMENTADO | Git, rama `develop`, suite Maven |
| RNF-PRU-01 | Deben existir pruebas unitarias para servicios criticos y reglas de negocio. | IMPLEMENTADO | Suite actual de servicios |
| RNF-PRU-02 | Deben existir pruebas de integracion web por rol con H2. | IMPLEMENTADO | `NexurIntegrationTest` |
| RNF-PRU-03 | Deben existir pruebas de seguridad negativas para rutas ADMIN, RESIDENTE y PORTERIA. | IMPLEMENTADO | Smoke local 403 y pruebas de integracion |
| RNF-PRU-04 | Deben existir pruebas del contrato del sandbox local y de la trazabilidad de resultados. | IMPLEMENTADO | `PagoServiceTest`, `PagoSimulacionServiceTest` |
| RNF-PRU-05 | Deben existir pruebas de navegador para los flujos principales antes de declarar release. | PENDIENTE | Falta automatizacion o checklist ejecutado con navegador |
| RNF-PRU-06 | Deben existir pruebas de carga, estres y recuperacion antes del despliegue. | PENDIENTE | No ejecutadas aun por alcance de la fase actual |

## 6. Matriz de permisos

| Funcion | ADMIN | RESIDENTE | PORTERIA |
| --- | --- | --- | --- |
| Inicio, login, logout, perfil y cambio de contrasena | Si | Si | Si |
| Registro publico | No aplica | Crea solo residente | No |
| Usuarios y roles | Si | No | No |
| Residentes | Si | No | No |
| Apartamentos | Si | No | No |
| Pagos globales y generacion de cuotas | Si | No | No |
| Pagos propios y sandbox | No aplica | Si | No |
| Confirmacion administrativa de efectivo/transferencia | Si | No | No |
| Factura de pago autorizado | Si | Solo propios | No |
| Estado de cuenta | No aplica | Solo propio | No |
| Reservas globales | Si | Solo propias | No |
| Crear reserva | No | Si | No |
| Aprobar/rechazar reserva | Si | No | No |
| Solicitar visitante | No | Si | No |
| Aprobar/rechazar/entrada/salida de visitante | No | No | Si |
| Catalogo de parqueaderos | Si | No | Operacion, no administracion |
| Vehiculos propios | No aplica | Si | Consulta/operacion global |
| Ingreso/salida e historial de parqueadero | No | No | Si |
| Incidencias propias | No aplica | Si | No |
| Gestionar estados de incidencias | Si | No | No |
| Comentarios y evidencias autorizadas | Si | Solo propias | No |
| Avisos | Publica y administra | Consulta | Consulta |
| Notificaciones | Consulta propias | Consulta propias | Consulta propias |
| Reportes y Excel | Si | No | No |
| Auditoria | Si | No | No |

## 7. Flujos de aceptacion prioritarios

### FA-01 Registro controlado y primer ingreso

1. ADMIN crea un apartamento con codigo de registro.
2. Una persona envia registro con datos validos y el codigo correcto.
3. El sistema crea usuario `RESIDENTE` y residente asociado al apartamento.
4. El primer login obliga a cambiar la contrasena.
5. El sistema permite continuar solo despues del cambio.
6. Se rechazan codigo incorrecto, correo duplicado, documento duplicado y apartamento inexistente.

### FA-02 Separacion estricta de roles

1. ADMIN puede entrar a usuarios, residentes, apartamentos, pagos, reservas, incidencias, reportes y auditoria.
2. RESIDENTE solo consulta y modifica sus propios flujos.
3. PORTERIA solo entra a visitantes y operacion de parqueaderos.
4. Las rutas prohibidas devuelven 403 o la pagina de acceso denegado.
5. PORTERIA no tiene residente, apartamento, cartera ni reservas.

### FA-03 Pago simulado y factura

1. ADMIN crea o genera un pago pendiente PSE o tarjeta.
2. RESIDENTE inicia el checkout y recibe una referencia.
3. El sandbox simula aprobado, pendiente, rechazado, anulado o error.
4. Solo aprobado cambia el pago a `PAGADO` y registra fecha de pago.
5. El residente descarga la factura PDF y solo ve sus registros.
6. Un resultado repetido, una identidad ajena o un método no simulable no altera el pago.

### FA-04 Solicitud y control de visitante

1. RESIDENTE crea solicitud con nombre y documento.
2. PORTERIA la aprueba o rechaza con motivo opcional.
3. Solo una aprobada permite registrar entrada.
4. Solo un visitante `DENTRO` permite registrar salida.
5. La salida deja el estado `FINALIZADA` y registra las fechas.

### FA-05 Incidencia con trazabilidad

1. RESIDENTE crea una incidencia.
2. ADMIN la pasa a revision, agrega respuesta y puede resolverla o cerrarla.
3. Las partes autorizadas agregan comentarios y evidencias permitidas.
4. Se generan notificaciones internas y, si SMTP esta habilitado, correo.
5. El residente no puede consultar la incidencia de otro residente.

## 8. Requisitos de datos y estados

### Estados de negocio

- Pago: `PENDIENTE`, `PAGADO`, `VENCIDO`.
- Reserva: `PENDIENTE`, `APROBADA`, `RECHAZADA`.
- Visitante: `PENDIENTE`, `APROBADA`, `RECHAZADA`, `DENTRO`, `FINALIZADA`.
- Incidencia: `ABIERTA`, `EN_REVISION`, `RESUELTA`, `CERRADA`.
- Parqueadero: `DISPONIBLE`, `ASIGNADO`, `OCUPADO`, `RESERVADO`, `MANTENIMIENTO`.
- Movimiento: `DENTRO`, `SALIO`.
- Vehiculo: `CARRO`, `MOTO`, `BICICLETA`.
- Metodo de pago: `PSE`, `TARJETA`, `EFECTIVO`, `TRANSFERENCIA`.

### Relaciones principales

- `Usuario` tiene un rol y puede tener un `Residente`.
- `Residente` pertenece a un `Apartamento` y puede tener pagos, reservas, visitantes,
  vehiculos e incidencias propios.
- `Pago` pertenece a un residente y a su apartamento.
- `Reserva` pertenece al residente solicitante y a su apartamento.
- `Visitante` pertenece al apartamento de la solicitud.
- `Parqueadero` puede asociarse a apartamento y vehiculo.
- `MovimientoParqueadero` vincula vehiculo y parqueadero y conserva entrada/salida.
- `Incidencia` tiene comentarios y adjuntos con control de acceso.

## 9. Plan de pruebas derivado

| Nivel | Objetivo | Requisitos cubiertos | Estado |
| --- | --- | --- | --- |
| Unitarias | Validar reglas de servicios, estados, montos, roles y archivos | RF-AUT, RF-PAG, RF-RES, RF-VIS, RF-PAR, RF-INC, RNF-SEG | IMPLEMENTADO |
| Integracion web | Validar rutas, formularios, CSRF, vistas y persistencia H2 | RF-AUT, RF-DAS y permisos por rol | IMPLEMENTADO |
| Contrato | Validar PDF, Excel, SMTP configurable y sandbox local de pagos | RF-PAG, RF-REP, RNF-SEG | IMPLEMENTADO |
| Navegador | Recorrer login, registro, pagos, reservas, visitantes, porteria e incidencias | Todos los flujos prioritarios | PENDIENTE |
| MySQL/Flyway | Validar esquema productivo, migraciones y datos reales de aceptacion | RNF-DAT, RNF-ARQ | PENDIENTE |
| Carga | Medir tiempos y concurrencia | RNF-REN | PENDIENTE |
| Estres | Encontrar punto de degradacion y recuperacion | RNF-REN, RNF-OPS | PENDIENTE |
| Despliegue | Levantar Spring Boot, MySQL y opcionalmente FastAPI en Docker | RNF-OPS-03 | IMPLEMENTADO localmente |

## 10. Criterio para declarar version final

No se debe declarar URBELIX como version final hasta cumplir los requisitos
`PENDIENTE` de prioridad alta, ejecutar los flujos FA-01 a FA-05 en navegador,
validar el perfil `prod` contra MySQL, probar migraciones Flyway, configurar el
respaldo/restauracion, medir rendimiento y verificar Docker.

El estado actual es una base funcional de desarrollo y aceptacion, no una
version productiva definitiva.
