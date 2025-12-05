# Build stage
FROM gradle:8.14.3-jdk21 AS builder

WORKDIR /app

# Copy Gradle files
COPY build.gradle.kts settings.gradle.kts gradle/ ./
COPY gradlew ./
COPY gradlew.bat ./
COPY gradle/wrapper/ gradle/wrapper/

# Copy all source code
COPY gateway/ ./gateway/
COPY outbox/ ./outbox/
COPY short-url/ ./short-url/
COPY short-url-stats/ ./short-url-stats/
COPY util/ ./util/

# Build all services
RUN ./gradlew clean build -x test --no-daemon

# Gateway service stage
FROM eclipse-temurin:21-jre-alpine AS gateway
WORKDIR /app
COPY --from=builder /app/gateway/build/libs/gateway.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# URL service stage
FROM eclipse-temurin:21-jre-alpine AS url-service
WORKDIR /app
COPY --from=builder /app/short-url/api/url-service/build/libs/url-service.jar ./app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

# Redirect service stage
FROM eclipse-temurin:21-jre-alpine AS redirect-service
WORKDIR /app
COPY --from=builder /app/short-url/api/redirect-service/build/libs/redirect-service.jar ./app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stats service stage
FROM eclipse-temurin:21-jre-alpine AS stats-service
WORKDIR /app
COPY --from=builder /app/short-url-stats/api/stats-service/build/libs/stats-service.jar ./app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

# Short URL stats batch worker stage
FROM eclipse-temurin:21-jre-alpine AS short-url-stats-batch
WORKDIR /app
COPY --from=builder /app/short-url-stats/batch/build/libs/batch.jar ./app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]

# Short URL stats consumer worker stage
FROM eclipse-temurin:21-jre-alpine AS short-url-stats-consumer
WORKDIR /app
COPY --from=builder /app/short-url-stats/consumer/build/libs/consumer.jar ./app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]

# Outbox polling publisher worker stage
FROM eclipse-temurin:21-jre-alpine AS outbox-worker
WORKDIR /app
COPY --from=builder /app/outbox/worker/build/libs/worker.jar ./app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]

