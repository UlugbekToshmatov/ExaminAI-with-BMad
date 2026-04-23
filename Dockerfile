# syntax=docker/dockerfile:1
# BuildKit: cache mounts speed rebuilds; enable via Docker Desktop (default) or DOCKER_BUILDKIT=1.
# Stage 1: build fat JAR (AC1) — no Maven in final image
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Dependencies only when pom changes; .m2 cache avoids re-downloading deps on every rebuild.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline -DskipTests

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package

# Stage 2: runtime — slim JRE only, plus curl for healthcheck (app AC4)
# Pin Temurin tag; bump when you want a newer Java 21 patch release.
FROM eclipse-temurin:21.0.10_7-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
# Wildcard keeps image in sync with pom.xml <version> (single repackaged jar from `mvn package`)
COPY --from=build /app/target/examin-ai-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
