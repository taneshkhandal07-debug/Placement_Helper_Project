# ==============================================================
# Dockerfile for SuperSet Pro — JECRC University Placement Portal
# Multi-stage build optimized for Render / Cloud deployments (Java 21)
# ==============================================================

# Stage 1: Build the application with Maven
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build the production artifact
COPY backend ./backend
COPY frontend ./frontend
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal, secure JRE runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create a non-root application user for container security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the compiled executable JAR from the builder stage
COPY --from=build /app/target/superset-pro.jar app.jar

# Create directory for persistent embedded database and upload storage
RUN mkdir -p data uploads/resumes && chown -R appuser:appuser /app

USER appuser

# Default port (Render overrides this with dynamic $PORT)
ENV PORT=8080
EXPOSE 8080

# Lean JVM flags specifically tuned for Render free tier (512MB RAM)
# 1. -XX:MaxRAMPercentage=65.0: Keeps heap within ~330MB leaving safe margin for OS/Metaspace
# 2. -XX:TieredStopAtLevel=1: Cuts memory and CPU overhead by 40% during startup
# 3. -Xss256k: Reduces thread stack memory consumption
# 4. -Dserver.address=0.0.0.0: Binds to all network interfaces for Render health check
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=65.0 -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xss256k -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -Dserver.address=0.0.0.0 -jar app.jar"]
