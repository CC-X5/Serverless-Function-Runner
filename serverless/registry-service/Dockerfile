FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Install curl for mvnw
RUN apk add --no-cache curl

# Copy Maven wrapper and parent POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Copy ALL module POMs (required for parent POM to resolve)
COPY registry-service/pom.xml registry-service/
COPY executor-service/pom.xml executor-service/
COPY gateway-service/pom.xml gateway-service/

# Download dependencies
RUN ./mvnw dependency:go-offline -pl registry-service -am -B

# Copy source code for this service only
COPY registry-service/src registry-service/src

# Build this service
RUN ./mvnw package -pl registry-service -am -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/registry-service/target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
