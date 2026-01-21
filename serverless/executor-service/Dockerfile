FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY executor-service/pom.xml executor-service/
RUN ./mvnw dependency:go-offline -pl executor-service -am -B
COPY executor-service/src executor-service/src
RUN ./mvnw package -pl executor-service -am -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /app/executor-service/target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/actuator/health || exit 1
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
