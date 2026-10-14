package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.application.notification.create.CreateNotificationUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final br.com.ofisy.notifications.application.notification.create.CreateNotificationUseCase createNotificationUseCase;

    public NotificationEventListener(br.com.ofisy.notifications.application.notification.create.CreateNotificationUseCase createNotificationUseCase) {
        this.createNotificationUseCase = createNotificationUseCase;
    }

    @SqsListener("${app.aws.sqs.notification-queue:techchallenge-ofisy-notifications-queue.fifo}")
    public void handleNotificationEvent(EventWrapper wrapper) {
        log.info("Received event: {}", wrapper.eventType());
        try {
            switch (wrapper.eventType()) {
                case "LOW_STOCK" -> handleLowStock(wrapper.eventId(), wrapper.payload());
                case "QUOTE_GENERATED" -> handleQuoteGenerated(wrapper.eventId(), wrapper.payload());
                default -> log.warn("Unknown event type: {}", wrapper.eventType());
            }
        } catch (Exception e) {
            log.error("Error processing notification event", e);
            throw e; // Rethrow to let SQS know the processing failed
        }
    }

    private void handleLowStock(String eventId, Object payload) {
        if (payload instanceof java.util.Map<?, ?> map) {
            UUID stockId = UUID.fromString((String) map.get("stockId"));
            String productName = (String) map.get("productName");
            Integer currentQuantity = (Integer) map.get("currentQuantity");
            Integer minThreshold = (Integer) map.get("minThreshold");

            br.com.ofisy.notifications.domain.notification.NotificationMessage message = br.com.ofisy.notifications.domain.notification.NotificationMessage.forLowStock(productName, currentQuantity, minThreshold);
            Notification notification = Notification.createForStock(UUID.fromString(eventId), stockId, message);
            
            persistEvent(notification);
        }
    }

    private void handleQuoteGenerated(String eventId, Object payload) {
        if (payload instanceof java.util.Map<?, ?> map) {
            UUID quoteId = UUID.fromString((String) map.get("quoteId"));
            UUID serviceOrderId = UUID.fromString((String) map.get("serviceOrderId"));
            BigDecimal totalPrice = new BigDecimal(map.get("totalPrice").toString());

            br.com.ofisy.notifications.domain.notification.NotificationMessage message = br.com.ofisy.notifications.domain.notification.NotificationMessage.forQuote(quoteId, serviceOrderId, totalPrice);
            Notification notification = Notification.createForQuote(UUID.fromString(eventId), quoteId, message);
            
            persistEvent(notification);
        }
    }

    private void persistEvent(Notification notification) {
        if (createNotificationUseCase.execute(notification)) {
            log.info("Notification saved for event: {}", notification.getId());
        } else {
            log.info("Event {} already processed, skipping", notification.getId());
        }
    }

    record EventWrapper(
        String eventId,
        String eventType,
        String serviceOrderId,
        String occurredAt,
        Object payload
    ) {}
}
