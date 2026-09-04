#!/usr/bin/env bash
# Arranca los dos procesos del contenedor: el servicio FastAPI de reportes
# (solo en loopback) y la aplicacion Spring Boot, que es la que expone el
# puerto publico. Si cualquiera de los dos muere, el contenedor termina para
# que Render lo reinicie en lugar de quedarse a medias.
set -euo pipefail

FASTAPI_PORT="${FASTAPI_PORT:-8000}"
PORT="${PORT:-8080}"

terminar() {
    trap - TERM INT
    kill 0
}
trap terminar TERM INT

echo "[urbelix] iniciando FastAPI en 127.0.0.1:${FASTAPI_PORT}"
uvicorn fastapi_reportes:app --host 127.0.0.1 --port "${FASTAPI_PORT}" &
pid_fastapi=$!

echo "[urbelix] iniciando Spring Boot en 0.0.0.0:${PORT}"
# shellcheck disable=SC2086
java ${JAVA_OPTS:-} -jar /app/urbelix.jar \
    --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-prod}" \
    --server.port="${PORT}" &
pid_spring=$!

# wait -n devuelve en cuanto uno de los dos procesos termina.
wait -n "${pid_fastapi}" "${pid_spring}"
codigo=$?
echo "[urbelix] un proceso termino con codigo ${codigo}; cerrando el contenedor"
kill 0
exit "${codigo}"
