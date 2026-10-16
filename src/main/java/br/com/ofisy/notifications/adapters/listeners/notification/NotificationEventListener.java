package br.com.ofisy.notifications.adapters.listeners.notification;
import br.com.ofisy.notifications.application.notification.createlowstock.CreateLowStockNotificationUseCase;
import br.com.ofisy.notifications.application.notification.createlowstock.CreateLowStockNotificationUseCase.CreateLowStockCommand;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase.CreateQuoteCommand;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Translates transport payloads only. Type, message and entity creation belong to application/domain. */
@Component
public class NotificationEventListener {
    private final CreateLowStockNotificationUseCase lowStock;
    private final CreateQuoteNotificationUseCase quote;
    public NotificationEventListener(CreateLowStockNotificationUseCase lowStock, CreateQuoteNotificationUseCase quote) {
        this.lowStock=lowStock;this.quote=quote;
    }
    @SqsListener("${app.aws.sqs.notification-queue:techchallenge-ofisy-notifications-queue.fifo}")
    public void handleNotificationEvent(EventWrapper event) {
        switch(event.eventType()) {
            case "LOW_STOCK" -> {
                var p=payload(event.payload());
                lowStock.executeEvent(UUID.fromString(event.eventId()),new CreateLowStockCommand(
                        UUID.fromString((String)p.get("stockId")),(String)p.get("productName"),
                        ((Number)p.get("currentQuantity")).intValue(),((Number)p.get("minThreshold")).intValue()));
            }
            case "QUOTE_GENERATED" -> {
                var p=payload(event.payload());
                quote.executeEvent(UUID.fromString(event.eventId()),new CreateQuoteCommand(
                        UUID.fromString((String)p.get("quoteId")),UUID.fromString((String)p.get("serviceOrderId")),
                        new BigDecimal(p.get("totalPrice").toString())));
            }
            default -> LoggerFactory.getLogger(getClass()).warn("Unknown event type: {}",event.eventType());
        }
    }
    private Map<?,?> payload(Object value) {
        if(value instanceof Map<?,?> map) return map;
        throw new IllegalArgumentException("Notification payload must be an object");
    }
    public record EventWrapper(String eventId,String eventType,String serviceOrderId,String occurredAt,Object payload) {}
}
