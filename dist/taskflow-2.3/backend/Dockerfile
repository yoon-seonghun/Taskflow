# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY lombok.config ./
COPY gradle ./gradle
COPY libs ./libs
COPY src ./src
RUN gradle build -x test --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# curl 설치 (헬스체크용)
RUN apk add --no-cache curl

COPY --from=build /app/build/libs/*.jar app.jar
# Tibero JDBC 드라이버 복사 (동적 로딩용)
COPY --from=build /app/libs ./libs

# 환경변수 기본값
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV TZ=Asia/Seoul

EXPOSE 8080

# JVM 옵션 적용하여 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Duser.timezone=$TZ -jar app.jar"]
