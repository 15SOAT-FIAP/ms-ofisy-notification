package br.com.ofisy.notifications.shared.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Continues, in the SQS listener, the trace of the service that published the message.
 * With the dd-java-agent and DD_TRACE_OTEL_ENABLED=true the OpenTelemetry API sees the Datadog spans;
 * without the agent (local and tests) it is a no-op and the action runs without a span.
 */
@Component
public class TraceContextPropagator {
    private static final String INSTRUMENTATION_NAME = "ms-ofisy-notification.listener";

    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        public String get(Map<String, String> carrier, String key) {
            return carrier == null ? null : carrier.get(key);
        }
    };

    public void runAsConsumer(Map<String, String> parentContext, String spanName, Map<String, String> attributes,
                              Runnable action) {
        if (parentContext == null || parentContext.isEmpty()) {
            action.run();
            return;
        }

        Context parent = GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.root(), parentContext, MAP_GETTER);
        SpanBuilder builder = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME)
                .spanBuilder(spanName)
                .setParent(parent)
                .setSpanKind(SpanKind.CONSUMER);
        attributes.forEach(builder::setAttribute);
        Span span = builder.startSpan();

        try (Scope ignored = span.makeCurrent()) {
            action.run();
        } catch (RuntimeException failure) {
            span.recordException(failure);
            throw failure;
        } finally {
            span.end();
        }
    }
}
