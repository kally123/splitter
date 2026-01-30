FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create receipts storage directory
RUN mkdir -p /app/receipts-data

COPY target/*.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m"

EXPOSE 8088

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
