# Despliegue de URBELIX en un VPS

Esta guia publica URBELIX con Docker Compose, MySQL persistente, FastAPI
interno y Caddy como proxy HTTPS. El dominio debe apuntar a la IP publica del
VPS antes de iniciar Caddy.

## Requisitos del VPS

- Ubuntu 22.04/24.04 LTS o una distribucion Linux equivalente.
- 2 vCPU y 4 GB de RAM como minimo.
- 8 GB de espacio libre como minimo; 20 GB es recomendable para imagenes,
  logs, datos y backups.
- Una IP publica fija.
- Un dominio o subdominio con registros DNS `A` apuntando al VPS.
- Puertos TCP `22`, `80` y `443` permitidos en el firewall.

Ejemplo de firewall con UFW:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

## Instalar Docker

Instala Docker Engine y el plugin Docker Compose siguiendo la documentacion
oficial de la distribucion. Verifica:

```bash
docker --version
docker compose version
```

Agrega el usuario de despliegue al grupo Docker si la administracion del VPS
lo permite:

```bash
sudo usermod -aG docker "$USER"
```

Cierra la sesion y vuelve a entrar para que el cambio tenga efecto.

## Preparar la aplicacion

```bash
git clone <URL_DEL_REPOSITORIO> urbelix
cd urbelix
cp .env.example .env
nano .env
```

Configura como minimo estos valores y reemplaza todos los ejemplos:

```env
APP_DOMAIN=urbelix.tudominio.com
APP_BASE_URL=https://urbelix.tudominio.com
MYSQL_DATABASE=nexur_db
MYSQL_USER=urbelix_app
MYSQL_PASSWORD=una-clave-larga-y-unica
MYSQL_ROOT_PASSWORD=otra-clave-larga-y-unica
SESSION_COOKIE_SECURE=true
REPORTS_FASTAPI_ENABLED=true
PAYMENTS_SIMULATION_ENABLED=true
ADMIN_BOOTSTRAP_ENABLED=false
```

No subas `.env` al repositorio. SMTP se agrega solo si se van a probar
correos reales, y los pagos permanecen en simulacion local.

## Iniciar en produccion

Ejecuta el preflight antes de crear contenedores:

```bash
chmod +x deploy/preflight-vps.sh
./deploy/preflight-vps.sh
```

Primero valida la composicion y luego construye los servicios:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml config -q
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

Caddy solicita y renueva automaticamente el certificado TLS cuando el DNS ya
resuelve al VPS y los puertos 80/443 estan accesibles.

Antes de iniciar, confirma que el dominio resuelve a la IP publica del VPS:

```bash
getent hosts urbelix.tudominio.com
```

La aplicacion queda disponible en:

```text
https://urbelix.tudominio.com/login
```

## Verificacion

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs --tail=100 app
curl -I https://urbelix.tudominio.com/login
```

Los tres servicios deben aparecer como `healthy` cuando termina el arranque.
El contenedor `app` no publica el puerto 8080 al exterior; solo Caddy puede
acceder a el dentro de la red Docker.

## Backups

Ejecuta un backup antes de migraciones y de forma periodica:

```bash
chmod +x deploy/backup-mysql.sh
./deploy/backup-mysql.sh
```

Los backups se guardan en `backups/`, que debe copiarse a un almacenamiento
externo. Restaurar un backup debe probarse en un entorno separado antes de
usarlo sobre la base de datos principal.

## Operacion

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f app
docker compose -f docker-compose.yml -f docker-compose.prod.yml pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml stop
```

No uses `down -v` en el servidor: elimina los volumenes de MySQL y de los
archivos subidos.

La composicion de produccion limita el consumo aproximado a 1 GB para Spring
Boot, 1 GB para MySQL, 256 MB para reportes y 128 MB para Caddy. Tambien rota
los logs de cada contenedor para evitar que llenen el disco.

## Pendiente para la publicacion real

La configuracion queda lista en el repositorio, pero la publicacion requiere
la IP, el dominio y el acceso SSH del VPS. Esos datos se configuran en el
servidor y no deben escribirse en el repositorio.
