FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# 의존성 설치를 위한 파일들만 먼저 복사 (레이어 캐싱)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여 및 의존성 다운로드 (소스 변경 시에도 캐시 유지됨)
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 실제 소스 복사 및 빌드
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul

ENTRYPOINT ["java", "-jar", "app.jar"]
