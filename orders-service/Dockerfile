FROM gradle:8.5-jdk21 AS build
WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./

RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -g 1001 -S orbita && \
    adduser -u 1001 -S orbita -G orbita

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown -R orbita:orbita /app

USER orbita
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]