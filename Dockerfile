FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system commonly && adduser --system --ingroup commonly commonly
COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar app.jar

USER commonly
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
