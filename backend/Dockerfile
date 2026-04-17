# ── Stage 1 : build ──────────────────────────────────────────────────────────
FROM gradle:9-jdk25 AS builder

WORKDIR /app

# Copie des fichiers de configuration Gradle en premier pour tirer parti du cache Docker
COPY settings.gradle.kts build.gradle.kts ./
RUN gradle dependencies --no-daemon || true

# Copie des sources
COPY src ./src

# Build du fat jar avec shadow plugin
RUN gradle shadowJar --no-daemon --stacktrace

# ── Stage 2 : runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Utilisateur non-root pour la sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copie du fat jar depuis le stage builder
COPY --from=builder /app/build/libs/carnetroute-backend-*-all.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/health || exit 1

CMD ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
