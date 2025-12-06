# Stage 1: Build stage using Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Runtime stage using lightweight JDK
FROM openjdk:17.0.1-jdk-slim

# Create non-root user for security
RUN groupadd -g 1000 spring && \
    useradd -u 1000 -g spring -s /bin/sh -m spring

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

ENV JAVA_OPTS="-Xmx512m -Xms256m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]