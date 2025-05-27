# Use OpenJDK 17 base image
FROM openjdk:17-jdk-slim

# Optional: Set a working directory
WORKDIR /app

# Copy the built jar (adjust name if needed)
COPY target/*.jar app.jar

# Expose the default port (change per service)
EXPOSE 8083

# Run the jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
