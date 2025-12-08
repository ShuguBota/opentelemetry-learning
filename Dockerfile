FROM amazoncorretto:25-alpine3.22

WORKDIR /app

EXPOSE 8080

COPY opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

ENV JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar"
ENV OTEL_TRACES_EXPORTER="logging"
ENV OTEL_METRICS_EXPORTER="logging"
ENV OTEL_LOGS_EXPORTER="logging"
ENV OTEL_METRIC_EXPORT_INTERVAL="15000"

COPY target/opentelemetry-example-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT [ "java", "-jar", "app.jar" ]