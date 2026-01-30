# Use Eclipse Temurin JDK 21 as base image
FROM eclipse-temurin:21-jre-alpine

# Add labels for image metadata
LABEL maintainer="Splitter Team"
LABEL description="User Service for Splitter application"
LABEL version="1.0.0"

# Create non-root user for security
RUN addgroup -S splitter && adduser -S splitter -G splitter

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R splitter:splitter /app

# Switch to non-root user
USER splitter

# Expose the port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# JVM options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
