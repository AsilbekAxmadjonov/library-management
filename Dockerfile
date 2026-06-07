# ── Stage 1: Build ─────────────────────────────────────────────────
# Use Maven + JDK 21 to compile and package the application
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory inside the container
WORKDIR /app

# Copy pom.xml first — Docker caches this layer
# If pom.xml didn't change, Maven dependencies are not re-downloaded
COPY pom.xml .

# Download all dependencies (cached layer — only re-runs if pom.xml changes)
RUN mvn dependency:go-offline -B

# Now copy the source code
COPY src ./src

# Build the JAR, skip tests (tests run in CI separately)
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run ───────────────────────────────────────────────────
# Use only JRE 21 (much smaller than full JDK) for running
FROM eclipse-temurin:21-jre-alpine

# Create a non-root user for security
# Never run applications as root inside containers
RUN addgroup -S library && adduser -S library -G library

# Set working directory
WORKDIR /app

# Copy only the built JAR from the builder stage
# This is why it's called multi-stage build — final image has no Maven, no source code
COPY --from=builder /app/target/library-management-system-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to non-root user
RUN chown library:library app.jar

# Switch to non-root user
USER library

# Expose the port the app runs on
EXPOSE 8080

# Health check — Docker will mark container as unhealthy if this fails
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Run the application
# -XX:+UseContainerSupport  → JVM respects Docker memory limits
# -XX:MaxRAMPercentage=75.0 → use max 75% of container memory
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]