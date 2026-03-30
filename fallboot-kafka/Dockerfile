FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
COPY fallboot-shared/pom.xml fallboot-shared/pom.xml
COPY fallboot-backend/pom.xml fallboot-backend/pom.xml
COPY fallboot-kafka/pom.xml fallboot-kafka/pom.xml
COPY fallboot-shared/src fallboot-shared/src
COPY fallboot-kafka/src fallboot-kafka/src
RUN mvn package -pl fallboot-kafka -am -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/fallboot-kafka/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]