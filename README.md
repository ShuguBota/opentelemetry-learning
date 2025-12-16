# OpenTelemetry Learning

Project done to understand better how Open Telemetry works.

## Running the project

Make sure the jar is up to date using:

```bash
mvn clean install
```

Run the whole thing using:

```bash
docker-compose up
```

## Useful paths

Application: http://localhost:8080/rolldice
Prometheus (Metrics): http://localhost:9090/
Jagger (Traces): http://localhost:16686/
File System (Logs): ./logs/otel-logs.txt

## Outcome

Throughout the course I learnt how to add OpenTelemetry to my application, but also how do these connect to different services like Prometheus, Jagger.
It was interesting exploring the steps needed to generate the metrics rather than just having them ready and just creating dashboards/alerts based on them.
Never used traces before so this was also fascinating to learn about.