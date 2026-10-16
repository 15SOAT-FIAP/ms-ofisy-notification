package br.com.ofisy.notifications.adapters.listeners.notification;
import br.com.ofisy.notifications.application.notification.createlowstock.CreateLowStockNotificationUseCase;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class NotificationEventListenerTest {
    private final CreateLowStockNotificationUseCase low=mock(CreateLowStockNotificationUseCase.class);
    private final CreateQuoteNotificationUseCase quote=mock(CreateQuoteNotificationUseCase.class);
    private final NotificationEventListener listener=new NotificationEventListener(low,quote);
    @Test void delegatesTypedLowStockCommandAndPropagatesFailures() {
        var id=UUID.randomUUID();var ref=UUID.randomUUID();
        var event=new NotificationEventListener.EventWrapper(id.toString(),"LOW_STOCK",null,null,
            Map.of("stockId",ref.toString(),"productName","Radiador","currentQuantity",2,"minThreshold",10));
        listener.handleNotificationEvent(event);
        verify(low).executeEvent(eq(id),argThat(c->c.stockId().equals(ref)&&c.currentQuantity()==2));
        verifyNoInteractions(quote);
        when(low.executeEvent(any(),any())).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(()->listener.handleNotificationEvent(event)).isInstanceOf(IllegalStateException.class);
    }
    @Test void delegatesQuoteCommand() {
        var id=UUID.randomUUID();var ref=UUID.randomUUID();
        listener.handleNotificationEvent(new NotificationEventListener.EventWrapper(id.toString(),"QUOTE_GENERATED",null,null,
            Map.of("quoteId",ref.toString(),"serviceOrderId",UUID.randomUUID().toString(),"totalPrice","123.45")));
        verify(quote).executeEvent(eq(id),argThat(c->c.quoteId().equals(ref)&&c.totalPrice().toString().equals("123.45")));
        verifyNoInteractions(low);
    }
    @Test void unknownTypeIsIgnoredAndInvalidKnownPayloadFails() {
        listener.handleNotificationEvent(new NotificationEventListener.EventWrapper(null,"UNKNOWN",null,null,null));
        verifyNoInteractions(low,quote);
        assertThatThrownBy(()->listener.handleNotificationEvent(new NotificationEventListener.EventWrapper(
            UUID.randomUUID().toString(),"LOW_STOCK",null,null,"invalid"))).isInstanceOf(IllegalArgumentException.class);
    }
}
