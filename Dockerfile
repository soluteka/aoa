# ============================================
# AOA AIX Logs Agent — Production Image
# ============================================
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# Cache dependencias
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

# Compilar
COPY src ./src
RUN ./mvnw -B clean package -DskipTests \
 && cp target/aoa-aix-logs-agent-*.jar /tmp/app.jar

# ============================================
FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="aoa-aix-logs-agent" \
      org.opencontainers.image.description="AIX telemetry ingestion agent (lparstat + errpt) → OTLP" \
      org.opencontainers.image.vendor="AOA Observability" \
      org.opencontainers.image.source="https://git.corp/observability/aoa-aix-logs-agent"

# Usuario no-root
RUN groupadd -r aoa -g 1001 && useradd -r -u 1001 -g aoa aoa \
 && mkdir -p /app/config /etc/aoa/keys \
 && chown -R aoa:aoa /app /etc/aoa

WORKDIR /app
COPY --from=build --chown=aoa:aoa /tmp/app.jar /app/app.jar

USER 1001

EXPOSE 8081 5140/udp 5141/tcp

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8081/actuator/health/liveness || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar --spring.config.additional-location=optional:file:/app/config/"]