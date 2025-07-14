
# 빌드 단계
FROM gradle:8.8-jdk21 AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src src
RUN gradle clean build -x test

# 실행 단계
FROM eclipse-temurin:21-alpine AS runner
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar Gitdeun.jar
RUN mkdir -p /app/logs

# 환경 변수 선언 (기본값)
ENV JAVA_OPTS=""

# 포트 설정
EXPOSE 8080

# 실행 명령에서 JAVA_OPTS 사용
CMD ["sh", "-c", "java $JAVA_OPTS -jar Gitdeun.jar"]