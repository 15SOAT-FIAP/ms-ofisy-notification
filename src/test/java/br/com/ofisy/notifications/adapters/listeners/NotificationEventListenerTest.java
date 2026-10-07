package br.com.ofisy.notifications.adapters.listeners;

import br.com.ofisy.notifications.application.usecases.NotificationUseCases;
import br.com.ofisy.notifications.domain.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationUseCases useCases;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void handleNotificationEvent_LowStock() {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        payload.put("stockId", UUID.randomUUID().toString());
        payload.put("productName", "Product");
        payload.put("currentQuantity", 5);
        payload.put("minThreshold", 10);

        when(useCases.findById(UUID.fromString(eventId))).thenReturn(Optional.empty());
        NotificationEventListener.EventWrapper wrapper = new NotificationEventListener.EventWrapper(eventId, "LOW_STOCK", UUID.randomUUID().toString(), "2026-10-07T00:00:00Z", payload);

        listener.handleNotificationEvent(wrapper);

        verify(useCases).save(any(Notification.class));
    }

    @Test
    void handleNotificationEvent_QuoteGenerated() {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        payload.put("quoteId", UUID.randomUUID().toString());
        payload.put("serviceOrderId", UUID.randomUUID().toString());
        payload.put("totalPrice", BigDecimal.TEN.toString());

        when(useCases.findById(UUID.fromString(eventId))).thenReturn(Optional.empty());
        NotificationEventListener.EventWrapper wrapper = new NotificationEventListener.EventWrapper(eventId, "QUOTE_GENERATED", UUID.randomUUID().toString(), "2026-10-07T00:00:00Z", payload);

        listener.handleNotificationEvent(wrapper);

        verify(useCases).save(any(Notification.class));
    }

    @Test
    void handleNotificationEvent_Unknown() {
        NotificationEventListener.EventWrapper wrapper = new NotificationEventListener.EventWrapper(UUID.randomUUID().toString(), "UNKNOWN", UUID.randomUUID().toString(), "2026-10-07T00:00:00Z", new Object());
        listener.handleNotificationEvent(wrapper);
        verify(useCases, never()).save(any(Notification.class));
    }

    
}
