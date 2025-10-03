# Build step
# Use Maven with JDK 17 as the build environment and name this stage "builder"
FROM maven:3.9.6-eclipse-temurin-17 AS builder
# Set the working directory inside the container to /app
WORKDIR /app
# Copy all project files from the host into the container
COPY . .
# Run Maven to build the project and create the JAR file, skipping tests
RUN mvn clean package -DskipTests

# Runtime step
# Use a lightweight JDK 17 runtime image for running the app
FROM eclipse-temurin:17-jdk-alpine
# Set the working directory inside the container to /app
WORKDIR /app
# Copy the built JAR file from the "builder" stage into this runtime image
COPY --from=builder /app/target/*.jar app.jar
# Command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]

