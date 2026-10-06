FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

# 운영 시간대는 여기서만 정해진다. 애플리케이션 코드에는 시간대를 고정하는 곳이 없다.
#
# 런타임에 TimeZone.setDefault()로 바꾸는 방식은 쓰지 않는다 — 그건 user.timezone
# 시스템 프로퍼티를 갱신하지 않아서 JDBC는 UTC로, 애플리케이션은 KST로 동작하고
# LocalDate가 DB 왕복에서 하루 밀린다 (#59에서 실제로 밟았다).
#
# 이 줄을 지우면 JRE 기본값인 UTC로 돌아가고 증명서 발급일이 전날로 인쇄된다.
# ENTRYPOINT의 -Duser.timezone과 이중으로 걸어 둔 이유다. 둘 중 하나만 남아도 동작한다.
ENV TZ=Asia/Seoul

WORKDIR /app

RUN addgroup --system commonly && adduser --system --ingroup commonly commonly
COPY --from=builder /workspace/build/libs/*-SNAPSHOT.jar app.jar

USER commonly
EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
