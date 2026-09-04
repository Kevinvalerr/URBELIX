#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

if [[ ! -f .env ]]; then
  echo "Falta .env. Copia .env.example y configura los secretos." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

: "${APP_DOMAIN:?Define APP_DOMAIN en .env}"
: "${APP_BASE_URL:?Define APP_BASE_URL en .env}"
: "${MYSQL_PASSWORD:?Define MYSQL_PASSWORD en .env}"
: "${MYSQL_ROOT_PASSWORD:?Define MYSQL_ROOT_PASSWORD en .env}"

if [[ "$APP_DOMAIN" == *"tudominio"* || "$APP_DOMAIN" == "localhost" ]]; then
  echo "APP_DOMAIN debe ser el dominio publico real del VPS." >&2
  exit 1
fi

if [[ "$APP_BASE_URL" != https://* ]]; then
  echo "APP_BASE_URL debe comenzar por https:// en produccion." >&2
  exit 1
fi

command -v docker >/dev/null || { echo "Docker no esta instalado." >&2; exit 1; }
docker compose version >/dev/null || { echo "Docker Compose no esta disponible." >&2; exit 1; }

if ! getent hosts "$APP_DOMAIN" >/dev/null; then
  echo "El dominio no resuelve por DNS: $APP_DOMAIN" >&2
  exit 1
fi

if ss -lnt | awk '{print $4}' | grep -Eq '(^|:)80$|(^|:)443$'; then
  echo "El puerto 80 o 443 ya esta ocupado. Deten el servicio que lo usa antes de desplegar." >&2
  exit 1
fi

AVAILABLE_KB="$(df -Pk "$PROJECT_DIR" | awk 'NR==2 {print $4}')"
if (( AVAILABLE_KB < 8388608 )); then
  echo "Advertencia: quedan menos de 8 GB libres en el disco." >&2
fi

docker compose -f docker-compose.yml -f docker-compose.prod.yml config -q
echo "Preflight correcto: dominio, HTTPS, Docker, puertos y composicion validos."
