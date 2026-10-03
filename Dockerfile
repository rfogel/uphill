FROM maven:3.9.11-eclipse-temurin-25 AS builder

WORKDIR /workspace/app

COPY pom.xml .
COPY src src

RUN mvn clean package -Dcheckstyle.skip --batch-mode --errors

FROM eclipse-temurin:25-jdk-alpine AS runtime
WORKDIR /app

COPY --from=builder /workspace/app/target/*.jar app.jar

EXPOSE 9090

USER 1000

ENTRYPOINT java \
    -XX:+UseContainerSupport \
    -XX:InitialRAMPercentage=70.0 \
    -XX:MaxRAMPercentage=70.0 \
    -XX:+UseStringDeduplication \
    -jar /app/app.jar