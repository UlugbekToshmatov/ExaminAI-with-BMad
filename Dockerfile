# syntax=docker/dockerfile:1
# Stage 1: build fat JAR (AC1) — no Maven in final image
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

# Stage 2: runtime — slim JRE only, plus curl for healthcheck (app AC4)
FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Wildcard keeps image in sync with pom.xml <version> (single repackaged jar from `mvn package`)
COPY --from=build /app/target/examin-ai-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
