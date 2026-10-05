package br.com.ofisy.notifications.adapters.listeners;

import br.com.ofisy.notifications.domain.Notification;
import br.com.ofisy.notifications.application.usecases.NotificationUseCases;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final br.com.ofisy.notifications.application.usecases.NotificationUseCases useCases;

    public NotificationEventListener(br.com.ofisy.notifications.application.usecases.NotificationUseCases useCases) {
        this.useCases = useCases;
    }

    @SqsListener("${app.aws.sqs.notification-queue:techchallenge-ofisy-notifications-queue}")
    public void handleNotificationEvent(EventWrapper wrapper) {
        log.info("Received event: {}", wrapper.eventType());
        try {
            switch (wrapper.eventType()) {
                case "LOW_STOCK" -> handleLowStock(wrapper.payload());
                case "QUOTE_GENERATED" -> handleQuoteGenerated(wrapper.payload());
                default -> log.warn("Unknown event type: {}", wrapper.eventType());
            }
        } catch (Exception e) {
            log.error("Error processing notification event", e);
            throw e; // Rethrow to let SQS know the processing failed
        }
    }

    private void handleLowStock(Object payload) {
        if (payload instanceof java.util.Map<?, ?> map) {
            UUID stockId = UUID.fromString((String) map.get("stockId"));
            String productName = (String) map.get("productName");
            Integer currentQuantity = (Integer) map.get("currentQuantity");
            Integer minThreshold = (Integer) map.get("minThreshold");

            br.com.ofisy.notifications.domain.NotificationMessage message = br.com.ofisy.notifications.domain.NotificationMessage.forLowStock(productName, currentQuantity, minThreshold);
            Notification notification = Notification.createForStock(stockId, message);
            
            useCases.save(notification);
            log.info("Notification saved for low stock: {}", stockId);
        }
    }

    private void handleQuoteGenerated(Object payload) {
        if (payload instanceof java.util.Map<?, ?> map) {
            UUID quoteId = UUID.fromString((String) map.get("quoteId"));
            UUID serviceOrderId = UUID.fromString((String) map.get("serviceOrderId"));
            BigDecimal totalPrice = new BigDecimal(map.get("totalPrice").toString());

            br.com.ofisy.notifications.domain.NotificationMessage message = br.com.ofisy.notifications.domain.NotificationMessage.forQuote(quoteId, serviceOrderId, totalPrice);
            Notification notification = Notification.createForQuote(quoteId, message);
            
            useCases.save(notification);
            log.info("Notification saved for quote generated: {}", quoteId);
        }
    }

    record EventWrapper(String eventType, Object payload) {}
}




