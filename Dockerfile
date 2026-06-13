# ── Stage 1: Build ──────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Maven wrapper and pom first (layer caching for dependencies)
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml

# Make the Maven wrapper executable
RUN chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the JAR (skip tests for faster builds)
RUN ./mvnw package -DskipTests -B

# ── Stage 2: Run ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Copy the SQLite database file
COPY database.db database.db

# Render uses the PORT environment variable
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT java -jar app.jar --server.port=${PORT}
