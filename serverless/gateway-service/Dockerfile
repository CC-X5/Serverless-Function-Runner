FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app


COPY pom.xml ./
COPY registry-service/pom.xml registry-service/
COPY executor-service/pom.xml executor-service/
COPY gateway-service/pom.xml gateway-service/


RUN mvn dependency:go-offline -pl gateway-service -am -B || true


COPY gateway-service/src gateway-service/src


RUN mvn package -pl gateway-service -am -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/gateway-service/target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
