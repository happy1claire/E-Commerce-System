# Stage 1: Build the application using Maven
FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Create the final, smaller image with only the JRE and the application JAR
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
