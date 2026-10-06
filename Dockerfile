FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

# 애플리케이션은 TimeZoneConfig가 기본 시간대를 고정하지만,
# JVM 기동 이전 로그와 OS 타임스탬프까지 맞추려면 컨테이너 TZ도 필요하다.
ENV TZ=Asia/Seoul

WORKDIR /app

RUN addgroup --system commonly && adduser --system --ingroup commonly commonly
COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar app.jar

USER commonly
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
