# syntax=docker/dockerfile:1

# --- Build: compile the Spring Boot jar (tests run in CI, not here) ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies first, so this layer is cached until pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
RUN ./mvnw -B -q -Dmaven.test.skip=true package \
    && cp target/backend-*.jar app.jar

# --- Runtime: JRE only, non-root user ---
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system vitta && useradd --system --gid vitta --no-create-home vitta

COPY --from=build /workspace/app.jar app.jar

# Fits a 512 MB container (Render Free).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

USER vitta
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
