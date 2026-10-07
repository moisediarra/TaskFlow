# syntax=docker/dockerfile:1

# --- Build stage: compile with the Maven wrapper and split the Spring Boot jar into cache-friendly layers.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Normalize line endings and the executable bit in case the context comes from a Windows checkout.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q -DskipTests package \
    && java -Djarmode=tools -jar target/taskflow-*.jar extract --layers --launcher --destination target/extracted

# --- Runtime stage: JRE only, non-root user.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S taskflow && adduser -S -G taskflow taskflow
WORKDIR /app

COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

USER taskflow
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
