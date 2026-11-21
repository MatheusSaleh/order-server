# syntax=docker/dockerfile:1.7
# Build: docker compose (order-platform) fornece o contexto adicional "grpc-commons".
# Standalone: docker build --build-context grpc-commons=../grpc-commons .
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /workspace
COPY --from=grpc-commons pom.xml /grpc-commons/pom.xml
COPY --from=grpc-commons src /grpc-commons/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f /grpc-commons/pom.xml install -DskipTests
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk upgrade --no-cache && addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/target/*.jar app.jar
USER app
EXPOSE 8081 9090
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5   CMD wget -qO- http://localhost:8081/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
