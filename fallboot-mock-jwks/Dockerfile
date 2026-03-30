FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/fallboot-mock-jwks-1.0.0.jar app.jar
EXPOSE 9999
ENTRYPOINT ["java", "-jar", "app.jar"]
