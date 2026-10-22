package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.notifications.application.notification.createlowstock.CreateLowStockNotificationUseCase;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationEventListenerTest {
    private final CreateLowStockNotificationUseCase low = mock(CreateLowStockNotificationUseCase.class);
    private final CreateQuoteNotificationUseCase quote = mock(CreateQuoteNotificationUseCase.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final NotificationEventListener listener = new NotificationEventListener(low, quote, mapper);

    private EventEnvelope event(String type, UUID serviceOrderId, Object payload) {
        return new EventEnvelope(UUID.randomUUID(), type, serviceOrderId, Instant.parse("2026-10-21T10:00:00Z"),
                mapper.valueToTree(payload));
    }

    @Test
    void delegatesTypedLowStockCommandAndPropagatesFailures() {
        var ref = UUID.randomUUID();
        var event = event("LOW_STOCK", null, Map.of("stockId", ref, "productName", "Radiador",
                "currentQuantity", 2, "minThreshold", 10));
        listener.handleNotificationEvent(event);
        verify(low).executeEvent(eq(event.eventId()), argThat(c -> c.stockId().equals(ref) && c.currentQuantity() == 2));
        verifyNoInteractions(quote);
        when(low.executeEvent(any(), any())).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> listener.handleNotificationEvent(event)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deserializesSharedEnvelopeAndDelegatesQuoteCommand() {
        var ref = UUID.randomUUID();
        var order = UUID.randomUUID();
        var original = event("QUOTE_GENERATED", order,
                Map.of("quoteId", ref, "serviceOrderId", order, "totalPrice", "123.45"));
        var decoded = mapper.readValue(mapper.writeValueAsString(original), EventEnvelope.class);
        assertThat(decoded.eventId()).isEqualTo(original.eventId());
        assertThat(decoded.occurredAt()).isEqualTo(original.occurredAt());
        listener.handleNotificationEvent(decoded);
        verify(quote).executeEvent(eq(original.eventId()), argThat(c ->
                c.quoteId().equals(ref) && c.serviceOrderId().equals(order) && c.totalPrice().toString().equals("123.45")));
        verifyNoInteractions(low);
    }

    @Test
    void unknownTypeIsIgnoredAndInvalidKnownPayloadFails() {
        listener.handleNotificationEvent(event("UNKNOWN", null, Map.of()));
        verifyNoInteractions(low, quote);
        assertThatThrownBy(() -> listener.handleNotificationEvent(event("LOW_STOCK", null, "invalid")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedOrMissingPayloadFieldsDoNotInvokeUseCases() {
        for (var payload : new Object[] {
                Map.of("stockId", "not-a-uuid", "productName", "X", "currentQuantity", 2, "minThreshold", 10),
                Map.of("stockId", UUID.randomUUID(), "productName", "X", "minThreshold", 10),
                Map.of("stockId", UUID.randomUUID(), "productName", "X", "currentQuantity", "invalid", "minThreshold", 10)}) {
            assertThatThrownBy(() -> listener.handleNotificationEvent(event("LOW_STOCK", null, payload)))
                    .isInstanceOf(RuntimeException.class);
        }
        verifyNoInteractions(low, quote);
    }

    @Test
    void mismatchedServiceOrderIsRejectedBeforeCreatingQuote() {
        var event = event("QUOTE_GENERATED", UUID.randomUUID(),
                Map.of("quoteId", UUID.randomUUID(), "serviceOrderId", UUID.randomUUID(), "totalPrice", "123.45"));
        assertThatThrownBy(() -> listener.handleNotificationEvent(event)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(low, quote);
    }

    @Test
    void invalidEnvelopeIdentifiersAndTimestampFailDeserialization() {
        var valid = mapper.writeValueAsString(event("UNKNOWN", null, Map.of()));
        var id = mapper.readTree(valid).get("eventId").asText();
        assertThatThrownBy(() -> mapper.readValue(valid.replace(id, "invalid"), EventEnvelope.class))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> mapper.readValue(valid.replace("2026-10-21T10:00:00Z", "invalid"), EventEnvelope.class))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> mapper.readValue("{}", EventEnvelope.class)).isInstanceOf(RuntimeException.class);
    }
}