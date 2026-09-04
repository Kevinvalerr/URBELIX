FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./

COPY src src

RUN mvn -B package -Dmaven.test.skip=true \
    && cp target/*SNAPSHOT.jar /tmp/urbelix.jar

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system app \
    && useradd --system --gid app --home-dir /app --create-home app

WORKDIR /app

COPY --from=build /tmp/urbelix.jar /app/app.jar

RUN mkdir -p /app/data/uploads \
    && chown -R app:app /app

USER app

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod
ENV UPLOAD_DIR=/app/data/uploads

EXPOSE 8080

VOLUME ["/app/data"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent "http://127.0.0.1:${PORT:-8080}/login" > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
