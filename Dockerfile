FROM eclipse-temurin:21-jdk-alpine@sha256:0bfc69a4758a86710e5c474032d28400a8bd00874766f9e8b1642ac2fd293159 AS build
WORKDIR /workspace
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
COPY src/main src/main
RUN --mount=type=cache,target=/root/.gradle chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine@sha256:51ab5e3302e7141ce665ca3ea85e8b5cd648eafbc3c0c90dd79d6537684e4555
RUN addgroup -S achat && adduser -S -G achat -u 10001 achat
WORKDIR /app
COPY --from=build --chown=achat:achat /workspace/build/libs/*.jar /app/achat.jar
USER achat
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/achat.jar"]
