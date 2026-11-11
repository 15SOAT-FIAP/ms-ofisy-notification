package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Translates transport payloads. Business rules remain in application/domain. */
@Component
public class NotificationEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventListener.class);
    private final Map<String, NotificationEventHandler> handlers;

    NotificationEventListener(List<NotificationEventHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                NotificationEventHandler::eventType, Function.identity()));
    }

    @SqsListener("${app.aws.sqs.notification-queue:ofisy-notification.fifo}")
    public void handleNotificationEvent(EventEnvelope event) {
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
}
