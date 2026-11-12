# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
# Cache ~/.m2 między buildami; testy idą w CI (potrzebują Dockera dla Testcontainers)
RUN --mount=type=cache,target=/root/.m2 \
    sh mvnw -q -B -DskipTests package \
    && cp target/rentengine-*.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home rentengine \
    && mkdir uploads && chown rentengine:rentengine uploads
COPY --from=build /build/app.jar app.jar
USER rentengine
ENV UPLOADS_DIR=/app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
