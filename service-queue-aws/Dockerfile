# Multi-stage build for Service Queue AWS
FROM openjdk:18-jdk-slim AS build

WORKDIR /app

# Copy Maven files
COPY pom.xml ./
COPY src ./src

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Build application
RUN mvn clean package -DskipTests

# Runtime stage using distroless
FROM gcr.io/distroless/java18-debian11:nonroot

WORKDIR /app

# Copy the JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Set JVM options for cloud environment
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC -XX:+UnlockExperimentalVMOptions -Xmx512m"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]