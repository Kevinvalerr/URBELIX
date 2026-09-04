# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Etapa 1: compilar el JAR de Spring Boot
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Se copia primero el POM para que la capa de dependencias se reutilice
# mientras no cambien las dependencias del proyecto.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package \
    && mv target/urbelix-*.jar target/urbelix.jar

# ---------------------------------------------------------------------------
# Etapa 2: imagen de ejecucion con JRE 21 + Python para el servicio de reportes
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-noble

ENV DEBIAN_FRONTEND=noninteractive \
    PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-venv curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Entorno virtual aislado para FastAPI/ReportLab.
COPY requirements-fastapi.txt .
RUN python3 -m venv /opt/venv \
    && /opt/venv/bin/pip install --no-cache-dir --upgrade pip \
    && /opt/venv/bin/pip install --no-cache-dir -r requirements-fastapi.txt
ENV PATH="/opt/venv/bin:${PATH}"

COPY fastapi_reportes.py .
COPY --from=build /build/target/urbelix.jar ./urbelix.jar
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Usuario sin privilegios; necesita escribir el SQLite de FastAPI en /app.
RUN useradd --system --create-home --uid 10001 urbelix \
    && chown -R urbelix:urbelix /app
USER urbelix

# Render inyecta PORT; 8080 es solo el valor por defecto para uso local.
ENV PORT=8080 \
    FASTAPI_PORT=8000 \
    FASTAPI_URL=http://127.0.0.1:8000 \
    JAVA_OPTS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC"

EXPOSE 8080

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
