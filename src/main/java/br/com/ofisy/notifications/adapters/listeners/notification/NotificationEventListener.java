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
        switch (event.type()) {
            case "LOW_STOCK" -> {
                requireObjectPayload(event);
                var payload = jsonMapper.treeToValue(event.payload(), LowStockPayload.class);
                lowStock.executeEvent(event.eventId(), new CreateLowStockCommand(
                        payload.stockId(), payload.productName(), payload.currentQuantity(), payload.minThreshold()));
            }
            case "QUOTE_GENERATED" -> {
                requireObjectPayload(event);
                if (event.serviceOrderId() == null) {
                    throw new IllegalArgumentException("serviceOrderId is required for QUOTE_GENERATED");
                }
                var payload = jsonMapper.treeToValue(event.payload(), QuoteGeneratedPayload.class);
                quote.executeEvent(event.eventId(), new CreateQuoteCommand(
                        payload.quoteId(), event.serviceOrderId(), payload.totalPrice()));
            }
            default -> LOGGER.warn("Ignoring event {} of unknown type {}", event.eventId(), event.type());
        }
    }

    private void requireObjectPayload(EventEnvelope event) {
        if (!event.payload().isObject()) {
            throw new IllegalArgumentException("Notification payload must be an object");
        }
    }
}
