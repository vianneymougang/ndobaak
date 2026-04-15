FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src/ src/
RUN mvn package -Dmaven.test.skip=true && \
    java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app
RUN apk add --no-cache gcompat libstdc++ && \
    addgroup -S spring && adduser -S spring -G spring
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./
USER spring
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=40s \
  CMD wget -qO- http://localhost:8080/api/v1/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
