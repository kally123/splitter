FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Splitter Team"
LABEL description="Settlement Service for Splitter application"
LABEL version="1.0.0"

RUN addgroup -S splitter && adduser -S splitter -G splitter

WORKDIR /app

COPY target/*.jar app.jar

RUN chown -R splitter:splitter /app

USER splitter

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
