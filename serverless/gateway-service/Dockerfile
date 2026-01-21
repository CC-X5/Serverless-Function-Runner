FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Install curl for mvnw
RUN apk add --no-cache curl

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY gateway-service/pom.xml gateway-service/
RUN ./mvnw dependency:go-offline -pl gateway-service -am -B
COPY gateway-service/src gateway-service/src
RUN ./mvnw package -pl gateway-service -am -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/gateway-service/target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
