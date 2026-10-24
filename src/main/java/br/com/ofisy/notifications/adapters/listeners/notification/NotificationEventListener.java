package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.notifications.adapters.listeners.dto.LowStockPayload;
import br.com.ofisy.notifications.adapters.listeners.dto.QuoteGeneratedPayload;
import br.com.ofisy.notifications.application.notification.createlowstock.CreateLowStockNotificationUseCase;
import br.com.ofisy.notifications.application.notification.createlowstock.CreateLowStockNotificationUseCase.CreateLowStockCommand;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase.CreateQuoteCommand;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Translates transport payloads. Business rules remain in application/domain. */
@Component
public class NotificationEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEventListener.class);
    private final CreateLowStockNotificationUseCase lowStock;
    private final CreateQuoteNotificationUseCase quote;
    private final JsonMapper jsonMapper;

    public NotificationEventListener(CreateLowStockNotificationUseCase lowStock,
                                     CreateQuoteNotificationUseCase quote, JsonMapper jsonMapper) {
        this.lowStock = lowStock;
        this.quote = quote;
        this.jsonMapper = jsonMapper;
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
        switch (event.type()) {
            case "StockLow" -> {
                requireObjectPayload(event);
                var payload = jsonMapper.treeToValue(event.payload(), LowStockPayload.class);
                boolean created = lowStock.executeEvent(event.eventId(), new CreateLowStockCommand(
                        payload.stockId(), payload.productName(), payload.currentQuantity(), payload.minThreshold()));
                logResult(event, payload.stockId(), created);
            }
            case "QuoteCreated" -> {
                requireObjectPayload(event);
                if (event.serviceOrderId() == null) {
                    throw new IllegalArgumentException("serviceOrderId is required for QuoteCreated");
                }
                var payload = jsonMapper.treeToValue(event.payload(), QuoteGeneratedPayload.class);
                boolean created = quote.executeEvent(event.eventId(), new CreateQuoteCommand(
                        payload.quoteId(), event.serviceOrderId(), payload.totalPrice()));
                logResult(event, payload.quoteId(), created);
            }
            default -> LOGGER.warn("Ignoring eventId={} type={} serviceOrderId={} outcome=unsupported",
                    event.eventId(), event.type(), event.serviceOrderId());
        }
    }

    private void logResult(EventEnvelope event, java.util.UUID referenceId, boolean created) {
        LOGGER.info("eventId={} type={} serviceOrderId={} referenceId={} outcome={}",
                event.eventId(), event.type(), event.serviceOrderId(), referenceId,
                created ? "created" : "duplicate");
    }

    private void requireObjectPayload(EventEnvelope event) {
        if (!event.payload().isObject()) {
            throw new IllegalArgumentException("Notification payload must be an object");
        }
    }
}
