# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home paywell
WORKDIR /app
COPY --from=build /src/target/paywell-backend-*.jar /app/app.jar
USER 10001
EXPOSE 8080
# Secrets (DB_PASSWORD, PAYWELL_JWT_SECRET, ...) are injected at runtime; nothing is baked into the image.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
