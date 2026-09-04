# Despliegue de URBELIX en Render

La aplicacion se despliega como **un unico servicio web Docker** en Render. La
imagen contiene los dos procesos del sistema:

- **Spring Boot** (`urbelix.jar`), que escucha en el puerto publico `$PORT`.
- **FastAPI/ReportLab** (`fastapi_reportes.py`), que escucha solo en
  `127.0.0.1:8000` y genera los PDFs que pide `ReporteController`.

Los lanza `docker-entrypoint.sh`; si cualquiera de los dos muere, el contenedor
termina y Render lo reinicia.

La base de datos **no vive en Render**: es un MySQL gestionado en Aiven.

## 1. Crear la base en Aiven

1. Crear un servicio *Aiven for MySQL*.
2. En la pestaña *Overview*, copiar los datos de conexion: `Host`, `Port`,
   `User`, `Password` y `Database name` (por defecto `defaultdb`).
3. Aiven solo acepta conexiones TLS. El perfil `prod` ya añade
   `?sslMode=REQUIRED` a la URL JDBC, no hay que tocar nada.

La base arranca vacia: Flyway aplicara `V1` a `V4` en el primer despliegue y
creara el esquema completo.

## 2. Crear el servicio en Render

Con el blueprint (`render.yaml`):

1. *New* -> *Blueprint*, apuntar al repo `Kevinvalerr/URBELIX`.
2. Render detecta `render.yaml` y crea el servicio `urbelix`.
3. Rellenar las variables marcadas como `sync: false` (ver tabla abajo).

O a mano: *New* -> *Web Service*, runtime **Docker**, `Dockerfile` en la raiz,
health check `/login`, y añadir las variables de entorno.

## 3. Variables de entorno

| Variable | Obligatoria | Valor |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | si | `prod,demo` en el primer despliegue; luego `prod` |
| `DB_HOST` | si | host de Aiven (`mysql-xxxx.aivencloud.com`) |
| `DB_PORT` | si | puerto de Aiven (no es 3306) |
| `DB_NAME` | si | `defaultdb` |
| `DB_USER` | si | `avnadmin` |
| `DB_PASSWORD` | si | contraseña de Aiven |
| `DB_POOL_SIZE` | no | `5` |
| `PORTAL_URL` | recomendada | `https://<tu-servicio>.onrender.com/login` |
| `DEMO_ADMIN_EMAIL` | solo con `demo` | correo del admin inicial |
| `DEMO_ADMIN_PASSWORD` | solo con `demo` | contraseña del admin inicial |
| `DEMO_RESIDENTE_PASSWORD` | solo con `demo` | contraseña de las cuentas sembradas |
| `MAIL_HOST` | no | servidor SMTP |
| `MAIL_PORT` | no | `587` |
| `MAIL_USERNAME` | no | usuario SMTP |
| `MAIL_PASSWORD` | no | contraseña SMTP |
| `JAVA_OPTS` | no | `-XX:MaxRAMPercentage=65 -XX:+UseSerialGC` |

`PORT` lo inyecta Render, no hay que definirla.

Sin las variables `MAIL_*` la app arranca igual; solo quedan inactivas las
notificaciones por correo de incidencias.

`PORTAL_URL` es el enlace que `EmailService` mete en los correos que reciben los
residentes. Si no se define apunta a `localhost` y llega inservible, asi que hay
que rellenarla con la URL publica de Render en cuanto el servicio exista.

## 4. Primer arranque: crear el usuario inicial

Con el perfil `prod` a secas **no se siembra ningun usuario**, y el formulario
de registro no crea cuentas (`POST /register` solo redirige: las cuentas de
residente se crean desde el modulo Residentes, ya autenticado). Sobre una base
vacia eso deja la aplicacion sin forma de entrar.

Para el primer despliegue hay que activar tambien el perfil `demo`, que siembra
un conjunto residencial completo mas un administrador:

```
SPRING_PROFILES_ACTIVE=prod,demo
DEMO_ADMIN_EMAIL=admin@urbelix.demo        (opcional, este es el valor por defecto)
DEMO_ADMIN_PASSWORD=<contraseña que elijas>
DEMO_RESIDENTE_PASSWORD=<contraseña que elijas>
```

`DemoDataInitializer` es idempotente: solo siembra si la tabla `apartamentos`
esta vacia, asi que los datos creados durante el uso sobreviven a los
redespliegues. Si las dos contraseñas no estan definidas, la aplicacion falla al
arrancar a proposito, para no crear cuentas con credenciales por defecto.

Cuando ya existan usuarios reales se puede quitar `demo` de
`SPRING_PROFILES_ACTIVE` y dejar solo `prod`.

## 5. Probar la imagen en local

```bash
docker build -t urbelix:local .

docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=... -e DB_PORT=... -e DB_NAME=defaultdb \
  -e DB_USER=avnadmin -e DB_PASSWORD=... \
  urbelix:local
```

La app queda en http://localhost:8080/login.

## Limitaciones del plan gratuito

- **512 MB de RAM.** Spring Boot mas uvicorn caben, pero sin margen. Si aparecen
  cierres por OOM, bajar `MaxRAMPercentage` o pasar a un plan de pago.
- **El servicio se duerme** tras 15 minutos sin trafico; el primer request
  despues tarda bastante (arranque de la JVM mas el de uvicorn).
- **Disco efimero.** El SQLite de FastAPI (`urbelix_fastapi.sqlite3`) se pierde
  en cada redespliegue. Solo afecta al CRUD de incidencias propio de FastAPI; el
  dominio real de incidencias vive en MySQL via la migracion V4.

## Comprobado antes de desplegar

Contra un MySQL 8.0.46 vacio, con el perfil `prod,demo` y `sslMode=REQUIRED`:

- Flyway aplica las cinco migraciones (`V1` a `V5`) sobre una base vacia.
- Hibernate valida el esquema resultante sin discrepancias
  (`spring.jpa.hibernate.ddl-auto=validate`).
- La semilla de demostracion crea 24 apartamentos, 8 residentes, 49 pagos,
  8 reservas, 6 visitantes y 6 incidencias con su historial.
- `GET /login` responde 200.
