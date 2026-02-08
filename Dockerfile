FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
COPY --from=build /target/CodeDuelZ-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
