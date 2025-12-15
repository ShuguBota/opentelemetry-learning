FROM amazoncorretto:25-alpine3.22

WORKDIR /app

EXPOSE 8080

# OpenTelemetry Java agent example, linking it to the jar of the application
# Zero code instrumentation
COPY opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

COPY target/opentelemetry-example-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT [ "java", "-jar", "app.jar" ]