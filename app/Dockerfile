# Build stage: compile and package the Spring Boot application with Maven and JDK 17.
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage: copy only the packaged JAR into a smaller Java 17 image.
FROM eclipse-temurin:17.0.15_6-jre
WORKDIR /app
COPY --from=builder /app/target/back-end-0.0.1-SNAPSHOT.jar app.jar

# Spring Boot serves HTTP traffic on port 8080.
EXPOSE 8080

# Start the Smart Clinic backend when the container launches.
ENTRYPOINT ["java", "-jar", "app.jar"]
