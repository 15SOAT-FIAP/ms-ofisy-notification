package br.com.ofisy.notifications.shared.tracing;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Without the dd-java-agent the OpenTelemetry API is a no-op: the action must always run. */
class TraceContextPropagatorTest {
    private static final Map<String, String> PARENT_CONTEXT =
            Map.of("traceparent", "00-6af793d30000000025cb7731d653ab37-6d0c2e20f86d8721-01");

    private final TraceContextPropagator propagator = new TraceContextPropagator();

    @Test
    void runsTheActionInsideTheParentContext() {
        var ran = new AtomicBoolean();

        propagator.runAsConsumer(PARENT_CONTEXT, "event.consume", Map.of("ofisy.event.type", "StockLow"), () -> ran.set(true));

        assertThat(ran).isTrue();
    }

    @Test
    void runsTheActionWhenThereIsNoParentContext() {
        var ran = new AtomicBoolean();

        propagator.runAsConsumer(Map.of(), "event.consume", Map.of(), () -> ran.set(true));

        assertThat(ran).isTrue();
    }

    @Test
    void propagatesTheFailureOfTheAction() {
        var failure = new IllegalStateException("unavailable");

        assertThatThrownBy(() -> propagator.runAsConsumer(PARENT_CONTEXT, "event.consume", Map.of(), () -> {
            throw failure;
        })).isSameAs(failure);
    }
}
