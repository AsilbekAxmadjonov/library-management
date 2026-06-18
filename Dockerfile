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

COPY src ./src

RUN mvn clean package -DskipTests -B


FROM eclipse-temurin:21-jre-alpine


RUN addgroup -S library && adduser -S library -G library

WORKDIR /app

COPY --from=builder /app/target/library-management-system-0.0.1-SNAPSHOT.jar app.jar

RUN chown library:library app.jar

USER library

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1


ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]