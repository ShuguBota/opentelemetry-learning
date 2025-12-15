package ro.cristian.opentelemetry_example;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;

@RestController
public class RollController {
  private static final Logger logger = LoggerFactory.getLogger(RollController.class);
  private final Meter meter;
  private final LongCounter requestCounter;

  
  public RollController() {
    OpenTelemetry openTelemetry = initOpenTelemetry();

    this.meter = openTelemetry.getMeter(RollController.class.getName());
    this.requestCounter = meter.counterBuilder("dice_roll_requests")
        .setDescription("Counts number of requests to /rolldice endpoint")
        .setUnit("1")
        .build();
  }

  static OpenTelemetry initOpenTelemetry() {
    Resource resource = Resource.create(Attributes.of(AttributeKey.stringKey("service.name"),"otel-java-app-code"));

    OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
        .setEndpoint("http://otel-collector:4317") // Same one as for the agent, but change from HTTP to gRPC
        .build();

    PeriodicMetricReader periodicMetricReader = PeriodicMetricReader.builder(metricExporter)
        .setInterval(Duration.ofSeconds(10))
        .build();

    SdkMeterProvider metricProvider = SdkMeterProvider.builder()
        .setResource(resource)
        .registerMetricReader(periodicMetricReader)
        .build(); 

    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
        .setMeterProvider(metricProvider)
        .build();

    Runtime.getRuntime().addShutdownHook(new Thread(sdk::close));

    return sdk;
  }


  @GetMapping("/rolldice")
  public String index(@RequestParam("player") Optional<String> player) {
    Dice dice = new Dice();

    int result = dice.roll(1, 6);

    if (player.isPresent()) {
      logger.info("{} is rolling the dice: {}", player.get(), result);
    } else {
      logger.info("Anonymous player is rolling the dice: {}", result);
    }

    requestCounter.add(1);

    return Integer.toString(result);
  }
}