# ==========================================
# Stage 1: Build application
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy Maven configuration
COPY pom.xml .

# Copy application source
COPY src ./src

# Build Spring Boot application
# Tests have already been verified separately.
RUN mvn -B clean package -DskipTests


# ==========================================
# Stage 2: Production runtime
# ==========================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user
RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring

# Copy packaged JAR from build stage
COPY --from=build --chown=spring:spring \
    /workspace/target/*.jar /app/app.jar

# Application configuration
ENV PORT=8080

# Cloud Run and other container platforms use PORT
EXPOSE 8080

# Run as non-root user
USER spring

# Start Spring Boot application
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar --server.port=${PORT:-8080}"]