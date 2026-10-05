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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationUseCases useCases;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void handleNotificationEvent_LowStock() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("stockId", UUID.randomUUID().toString());
        payload.put("productName", "Product");
        payload.put("currentQuantity", 5);
        payload.put("minThreshold", 10);

        NotificationEventListener.EventWrapper wrapper = new NotificationEventListener.EventWrapper("LOW_STOCK", payload);

        listener.handleNotificationEvent(wrapper);

        verify(useCases).save(any(Notification.class));
    }

    @Test
    void handleNotificationEvent_QuoteGenerated() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("quoteId", UUID.randomUUID().toString());
        payload.put("serviceOrderId", UUID.randomUUID().toString());
        payload.put("totalPrice", BigDecimal.TEN.toString());

        NotificationEventListener.EventWrapper wrapper = new NotificationEventListener.EventWrapper("QUOTE_GENERATED", payload);

        listener.handleNotificationEvent(wrapper);

        verify(useCases).save(any(Notification.class));
    }

    @Test
    void handleNotificationEvent_Unknown() {
        NotificationEventListener.EventWrapper wrapper = new NotificationEventListener.EventWrapper("UNKNOWN", new Object());
        listener.handleNotificationEvent(wrapper);
        verify(useCases, never()).save(any(Notification.class));
    }

    
}

