package io.diagrid.dapr.otel;

import io.diagrid.dapr.PizzaDelivery;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class OpenTelemetryConfig {

  
  private static int COLLECTOR_PORT = 9411;
  private static String COLLECTOR_HOST = "otel-collector.default.svc.cluster.local";

  private static final String ENDPOINT_V2_SPANS = "/api/v2/spans";

  @Bean
  public OpenTelemetry initOpenTelemetry() {
    return createOpenTelemetry();
  }

  @Bean
  public Tracer initTracer(@Autowired OpenTelemetry openTelemetry) {
    return openTelemetry.getTracer(PizzaDelivery.class.getCanonicalName());
  }

  /**
   * Creates an opentelemetry instance.
   * @return OpenTelemetry.
   */
  public static OpenTelemetry createOpenTelemetry() {
    // Only exports to Zipkin if it is up. Otherwise, ignore it.
    // This is helpful to avoid exceptions for examples that do not require Zipkin.
    //if (isZipkinUp()) {
      String httpUrl = String.format("http://%s:%d", COLLECTOR_HOST, COLLECTOR_PORT);
      ZipkinSpanExporter zipkinExporter =
          ZipkinSpanExporter.builder()
              .setEndpoint(httpUrl + ENDPOINT_V2_SPANS)
              .setServiceName(PizzaDelivery.class.getName())
              .build();

      SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(zipkinExporter))
          .build();

      return OpenTelemetrySdk.builder()
          .setTracerProvider(sdkTracerProvider)
          .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
          .buildAndRegisterGlobal();
    // } else {
    //   System.out.println("WARNING: Zipkin is not available.");
    // }

    //return null;
  }

  /**
   * Converts current OpenTelemetry's context into Reactor's context.
   * @return Reactor's context.
   */
  public static reactor.util.context.ContextView getReactorContext() {
    return getReactorContext(Context.current());
  }

  /**
   * Converts given OpenTelemetry's context into Reactor's context.
   * @param context OpenTelemetry's context.
   * @return Reactor's context.
   */
  public static reactor.util.context.Context getReactorContext(Context context) {
    Map<String, String> map = new HashMap<>();
    TextMapPropagator.Setter<Map<String, String>> setter =
        (carrier, key, value) -> map.put(key, value);

    GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(context, map, setter);
    reactor.util.context.Context reactorContext = reactor.util.context.Context.empty();
    for (Map.Entry<String, String> entry : map.entrySet()) {
      reactorContext = reactorContext.put(entry.getKey(), entry.getValue());
    }
    return reactorContext;
  }

  public static void addContextToHeaders(Context context, HttpHeaders headers) {
    Map<String, String> map = new HashMap<>();
    TextMapPropagator.Setter<Map<String, String>> setter =
        (carrier, key, value) -> map.put(key, value);

    GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(context, map, setter);
    
    for (Map.Entry<String, String> entry : map.entrySet()) {
      headers.add(entry.getKey(), entry.getValue());
    }
   
  }

  private static boolean isZipkinUp() {
    try (Socket ignored = new Socket(COLLECTOR_HOST, COLLECTOR_PORT)) {
      return true;
    } catch (IOException ignored) {
      return false;
    }
  }
}
