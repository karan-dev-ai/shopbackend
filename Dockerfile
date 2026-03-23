# Use Java 17
FROM eclipse-temurin:17-jdk-alpine

# Set working directory
WORKDIR /app

# Copy project files
COPY .. .

# Give permission
RUN chmod +x mvnw

# Build the project
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8080

# Run app
CMD ["java", "-jar", "target/*.jar"]