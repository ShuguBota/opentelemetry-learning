package ro.cristian.opentelemetry_example;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

@RestController
public class RollController {
  private static final Logger logger = LoggerFactory.getLogger(RollController.class);
  /**
   * Same one as for the agent, but change from HTTP to gRPC
   */
  private final static String OTEL_EXPORTER_OTLP_ENDPOINT = "http://otel-collector:4317";
  private final static String INSTRUMENTATION_NAME = RollController.class.getName();
  
  private final Meter meter;
  private final LongCounter requestCounter;

  private final Tracer tracer;
  private Context context;

  
  public RollController() {
    // OpenTelemetry openTelemetry = initOpenTelemetry();
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();

    this.meter = openTelemetry.getMeter(INSTRUMENTATION_NAME);
    this.requestCounter = meter.counterBuilder("dice_roll_requests")
        .setDescription("Counts number of requests to /rolldice endpoint")
        .setUnit("1")
        .build();

    this.tracer = openTelemetry.getTracer(INSTRUMENTATION_NAME);

    // Set up logging instrumentation
    OpenTelemetryAppender.install(openTelemetry);
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  /**
   * Initialize OpenTelemetry SDK with OTLP exporters for metrics, traces, and logs.
   * If you use the OpenTelemetry Java Agent, this initialization is not needed as the agent
   * handles it automatically.
   */
  static OpenTelemetry initOpenTelemetry() {
    Resource resource = Resource.create(Attributes.of(AttributeKey.stringKey("service.name"),"otel-java-app-code"));

    // Metrics
    OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
        .setEndpoint(OTEL_EXPORTER_OTLP_ENDPOINT)
        .build();

    PeriodicMetricReader periodicMetricReader = PeriodicMetricReader.builder(metricExporter)
        .setInterval(Duration.ofSeconds(10))
        .build();

    SdkMeterProvider metricProvider = SdkMeterProvider.builder()
        .setResource(resource)
        .registerMetricReader(periodicMetricReader)
        .build();

    // Traces
    OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
        .setEndpoint(OTEL_EXPORTER_OTLP_ENDPOINT)
        .build();

    SimpleSpanProcessor spanProcessor = SimpleSpanProcessor.builder(spanExporter).build();

    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .setResource(resource)
        .addSpanProcessor(spanProcessor)
        .build();

    // Logs
    OtlpGrpcLogRecordExporter logExporter = OtlpGrpcLogRecordExporter.builder()
        .setEndpoint(OTEL_EXPORTER_OTLP_ENDPOINT)
        .build();

    BatchLogRecordProcessor logRecordProcessor = BatchLogRecordProcessor.builder(logExporter).build();
    
    SdkLoggerProvider loggerProvider;
      loggerProvider = SdkLoggerProvider.builder()
              .setResource(resource)
              .addLogRecordProcessor(logRecordProcessor)
              .build();

    // Builidng the SDK
    OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
        .setMeterProvider(metricProvider)
        .setTracerProvider(tracerProvider)
        .setLoggerProvider(loggerProvider)
        .build();


    // Shutdown hook to ensure metrics are flushed before application exit
    Runtime.getRuntime().addShutdownHook(new Thread(sdk::close));

    return sdk;
  }


  @GetMapping("/rolldice")
  public String index(@RequestParam("player") Optional<String> player) {
    logger.info("Received request to /rolldice endpoint");

    // Starting a span
    Span span = tracer.spanBuilder("roll_dice_operation").setNoParent().startSpan();
    span.makeCurrent();
    context = Context.current().with(span);

    Dice dice = new Dice();

    int result = dice.roll(1, 6);

    if (player.isPresent()) {
      logger.info("{} is rolling the dice: {}", player.get(), result);
    } else {
      logger.info("Anonymous player is rolling the dice: {}", result);
    }

    work();

    requestCounter.add(1);

    // Ending the span
    span.end();

    return Integer.toString(result);
  }

  private void work() {
    Span span = tracer.spanBuilder("work").setParent(context).startSpan();
    context = Context.current().with(span);

    // If you would want to send it to another application you would inject the context in the header
    // e.g.
    // W3CTraceContextPropagator propagator = W3CTraceContextPropagator.getInstance();
    // propagator.inject(context, httpPost, HTTPPost::setHeader);
    // You do not need the propagator if you use the Java agent

    try {
      logger.info("Doing some work...");
      Thread.sleep(100);
      logger.info("Work done.");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    span.end();
  }
}