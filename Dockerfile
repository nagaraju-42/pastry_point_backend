# =====================================================
# BakeryQ Backend — Dockerfile
# Multi-stage build: smaller final image, faster startup
# =====================================================

# ── Stage 1: Build with Maven ─────────────────────
FROM maven:3.9.4-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first so Docker caches dependencies separately
# (only re-downloads if pom.xml changes — saves 5 min per rebuild)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run with slim JRE ────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S bakeryq && adduser -S bakeryq -G bakeryq

# Copy only the JAR from the build stage
COPY --from=builder /app/target/bakeryq-backend-0.0.1-SNAPSHOT.jar app.jar

# Set ownership
RUN chown bakeryq:bakeryq app.jar
USER bakeryq

# Expose backend port
EXPOSE 8080

# Health check — Render uses this to know if app is alive
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/auth/health || exit 1

# Start command — activates production profile
ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
