FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# 빌드 캐시를 위해 의존성 파일들 먼저 복사
COPY build.gradle settings.gradle ./

# 소스 코드 복사 및 빌드
COPY src src
RUN gradle bootJar -x test

# 실행 환경 (JRE만 포함하여 경량화)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌더에서 생성된 jar 파일 복사
COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar

# 8081 포트 노출
EXPOSE 8081

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
