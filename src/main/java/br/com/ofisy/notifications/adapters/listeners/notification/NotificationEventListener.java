package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.notifications.shared.tracing.MessageTraceContextReader;
import br.com.ofisy.notifications.shared.tracing.TraceContextPropagator;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Translates transport payloads. Business rules remain in application/domain. */
@Component
public class NotificationEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventListener.class);
    private static final String CONSUME_SPAN_NAME = "event.consume";
    private final Map<String, NotificationEventHandler> handlers;
    private final MessageTraceContextReader traceContextReader;
    private final TraceContextPropagator traceContextPropagator;

    NotificationEventListener(List<NotificationEventHandler> handlers, MessageTraceContextReader traceContextReader,
                              TraceContextPropagator traceContextPropagator) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                NotificationEventHandler::eventType, Function.identity()));
        this.traceContextReader = traceContextReader;
        this.traceContextPropagator = traceContextPropagator;
    }

    /** Continues the publisher's trace so the service order flow stays in one trace across the microservices. */
    @SqsListener("${app.aws.sqs.notification-queue:ofisy-notification.fifo}")
    public void handleNotificationEvent(EventEnvelope event,
                                        @Header(name = MessageTraceContextReader.TRACE_CONTEXT_ATTRIBUTE, required = false)
                                        Object traceContext) {
        traceContextPropagator.runAsConsumer(traceContextReader.read(traceContext), CONSUME_SPAN_NAME,
                spanAttributes(event), () -> handle(event));
    }

    private void handle(EventEnvelope event) {
        try {
            dispatch(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("eventId={} type={} serviceOrderId={} outcome=failed",
                    event.eventId(), event.type(), event.serviceOrderId());
            throw failure;
        }
    }

    private void dispatch(EventEnvelope event) {
        var handler = handlers.get(event.type());
        if (handler == null) {
            LOGGER.warn("Ignoring eventId={} type={} serviceOrderId={} outcome=unsupported",
                    event.eventId(), event.type(), event.serviceOrderId());
            return;
        }
        if (!event.payload().isObject()) {
            throw new IllegalArgumentException("Notification payload must be an object");
        }
        var result = handler.handle(event);
        LOGGER.info("eventId={} type={} serviceOrderId={} referenceId={} outcome={}",
                event.eventId(), event.type(), event.serviceOrderId(), result.referenceId(),
                result.created() ? "created" : "duplicate");
    }

    private Map<String, String> spanAttributes(EventEnvelope event) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("ofisy.event.id", String.valueOf(event.eventId()));
        attributes.put("ofisy.event.type", String.valueOf(event.type()));
        if (event.serviceOrderId() != null) {
            attributes.put("ofisy.service_order.id", event.serviceOrderId().toString());
        }
        return attributes;
    }
}
