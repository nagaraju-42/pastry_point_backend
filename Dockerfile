# =====================================================
# BakeryQ Backend — Dockerfile (FIXED for Render)
# =====================================================

# ── Stage 1: Build with Maven ─────────────────────
FROM maven:3.9.4-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run ─────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S bakeryq && adduser -S bakeryq -G bakeryq

COPY --from=builder /app/target/bakeryq-backend-0.0.1-SNAPSHOT.jar app.jar

RUN chown bakeryq:bakeryq app.jar
USER bakeryq

# ⚠️ IMPORTANT: expose dynamic port
EXPOSE 10000

# ✅ Health check using dynamic port
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:$PORT/api/auth/health || exit 1

# ✅ FIX: bind Spring Boot to Render port
ENTRYPOINT ["sh", "-c", "java \
  -Dspring.profiles.active=prod \
  -Dserver.port=$PORT \
  -Djava.security.egd=file:/dev/./urandom \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -jar app.jar"]