# Build stage
FROM gradle:8.14.3-jdk21 AS builder

WORKDIR /app

# Copy Gradle files
COPY build.gradle.kts settings.gradle.kts gradle/ ./
COPY gradlew ./
COPY gradlew.bat ./
COPY gradle/wrapper/ gradle/wrapper/

# Copy all source code
COPY api/ ./api/
COPY domain/ ./domain/
COPY util/ ./util/
COPY worker/ ./worker/

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
COPY --from=builder /app/api/url-service/build/libs/url-service.jar ./app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

# Redirect service stage
FROM eclipse-temurin:21-jre-alpine AS redirect-service
WORKDIR /app
COPY --from=builder /app/api/redirect-service/build/libs/redirect-service.jar ./app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stats service stage
FROM eclipse-temurin:21-jre-alpine AS stats-service
WORKDIR /app
COPY --from=builder /app/api/stats-service/build/libs/stats-service.jar ./app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

# Short URL stats batch worker stage
FROM eclipse-temurin:21-jre-alpine AS short-url-stats-batch
WORKDIR /app
COPY --from=builder /app/worker/short-url-stats-batch/build/libs/short-url-stats-batch.jar ./app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]

# Short URL stats consumer worker stage
FROM eclipse-temurin:21-jre-alpine AS short-url-stats-consumer
WORKDIR /app
COPY --from=builder /app/worker/short-url-stats-consumer/build/libs/short-url-stats-consumer.jar ./app.jar
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]

# Outbox polling publisher worker stage
FROM eclipse-temurin:21-jre-alpine AS outbox-polling-publisher
WORKDIR /app
COPY --from=builder /app/worker/outbox-polling-publisher/build/libs/outbox-polling-publisher.jar ./app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]

