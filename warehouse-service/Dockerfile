# Use Amazon Corretto (AWS's OpenJDK distribution)
FROM amazoncorretto:17-alpine

# Set working directory
WORKDIR /app

# Copy the jar file from Gradle build
# Gradle puts jars in build/libs/ (not target/ like Maven)
COPY build/libs/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]