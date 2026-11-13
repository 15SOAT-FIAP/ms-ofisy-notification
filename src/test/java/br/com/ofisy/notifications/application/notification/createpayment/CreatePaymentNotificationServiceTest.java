package br.com.ofisy.notifications.application.notification.createpayment;

import br.com.ofisy.notifications.application.notification.createquote.ServiceOrderCustomerLookup;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class CreatePaymentNotificationServiceTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final ServiceOrderCustomerLookup lookup = mock(ServiceOrderCustomerLookup.class);
    private final CreatePaymentNotificationService service = new CreatePaymentNotificationService(repository, lookup);
    private final UUID order = UUID.randomUUID();
    private final UUID payment = UUID.randomUUID();

    @Test
    void checkoutPersistsTheOwnerAndReturnsTheAtomicInsertResult() {
        var event = UUID.randomUUID();
        var customer = UUID.randomUUID();
        var url = "https://www.mercadopago.com.br/checkout/v1/redirect?order_id=local";
        when(lookup.findCustomerId(order)).thenReturn(customer);
        when(repository.createIfAbsent(any())).thenReturn(false);

        assertThat(service.checkoutCreated(event, order, payment, url, new BigDecimal("250.00"))).isFalse();
        verify(repository).createIfAbsent(argThat(notification -> notification.getId().equals(event)
                && notification.getType() == NotificationType.PAYMENT_CHECKOUT_CREATED
                && notification.getPaymentId().equals(payment)
                && notification.getCustomerId().equals(customer)
                && notification.getCheckoutUrl().equals(url)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://www.mercadopago.com.br/checkout",
            "https://mercadopago.com.br.example.org/checkout",
            "https://example.org@www.mercadopago.com.br/checkout"
    })
    void invalidCheckoutNeverReachesPersistence(String url) {
        assertThatThrownBy(() -> service.checkoutCreated(UUID.randomUUID(), order, payment, url, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(lookup, repository);
    }
}
