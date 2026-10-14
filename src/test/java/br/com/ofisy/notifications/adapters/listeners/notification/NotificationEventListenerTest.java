package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.application.notification.create.CreateNotificationUseCase;
import br.com.ofisy.notifications.domain.notification.Notification;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private CreateNotificationUseCase createNotificationUseCase;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    void handleNotificationEvent_LowStock() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("stockId", UUID.randomUUID().toString());
        payload.put("productName", "Product");
        payload.put("currentQuantity", 5);
        payload.put("minThreshold", 10);

        NotificationEventListener.EventWrapper wrapper = new NotificationEventListener.EventWrapper(UUID.randomUUID().toString(), "LOW_STOCK", UUID.randomUUID().toString(), "2023-01-01T00:00:00Z", payload);

        listener.handleNotificationEvent(wrapper);

        verify(createNotificationUseCase).execute(any(Notification.class));
    }

    @Test
    void handleNotificationEvent_QuoteGenerated() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("quoteId", UUID.randomUUID().toString());
        payload.put("serviceOrderId", UUID.randomUUID().toString());
        payload.put("totalPrice", BigDecimal.TEN.toString());

        NotificationEventListener.EventWrapper wrapper = new NotificationEventListener.EventWrapper(UUID.randomUUID().toString(), "QUOTE_GENERATED", UUID.randomUUID().toString(), "2023-01-01T00:00:00Z", payload);

        listener.handleNotificationEvent(wrapper);

        verify(createNotificationUseCase).execute(any(Notification.class));
    }

    @Test
    void handleNotificationEvent_Unknown() {
        NotificationEventListener.EventWrapper wrapper = new NotificationEventListener.EventWrapper(UUID.randomUUID().toString(), "UNKNOWN", UUID.randomUUID().toString(), "2023-01-01T00:00:00Z", new Object());
        listener.handleNotificationEvent(wrapper);
        verify(createNotificationUseCase, never()).execute(any());
    }

    @Test
    void persistenceFailureIsPropagatedForRetry() {
        var payload = Map.<String, Object>of("stockId", UUID.randomUUID().toString(),
                "productName", "Product", "currentQuantity", 5, "minThreshold", 10);
        var wrapper = new NotificationEventListener.EventWrapper(UUID.randomUUID().toString(),
                "LOW_STOCK", null, "2026-01-01T00:00:00Z", payload);
        when(createNotificationUseCase.execute(any())).thenThrow(new IllegalStateException("DynamoDB unavailable"));
        assertThrows(IllegalStateException.class, () -> listener.handleNotificationEvent(wrapper));
    }

    @Test
    void duplicateEventDoesNotUpdateExistingNotification() {
        var payload = Map.<String, Object>of("quoteId", UUID.randomUUID().toString(),
                "serviceOrderId", UUID.randomUUID().toString(), "totalPrice", "10.00");
        var wrapper = new NotificationEventListener.EventWrapper(UUID.randomUUID().toString(),
                "QUOTE_GENERATED", null, "2026-01-01T00:00:00Z", payload);
        when(createNotificationUseCase.execute(any())).thenReturn(false);
        listener.handleNotificationEvent(wrapper);
        verify(createNotificationUseCase).execute(any());
    }

    
}
