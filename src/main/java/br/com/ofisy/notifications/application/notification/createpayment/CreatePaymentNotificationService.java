package br.com.ofisy.notifications.application.notification.createpayment;

import br.com.ofisy.notifications.application.notification.createquote.ServiceOrderCustomerLookup;
import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.NotificationMessage;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Service
class CreatePaymentNotificationService implements CreatePaymentNotificationUseCase {
    private final NotificationRepository repository;
    private final ServiceOrderCustomerLookup customerLookup;

    CreatePaymentNotificationService(NotificationRepository repository, ServiceOrderCustomerLookup customerLookup) {
        this.repository = repository;
        this.customerLookup = customerLookup;
    }

    @Override
    public boolean checkoutCreated(UUID eventId, UUID serviceOrderId, UUID paymentId, String checkoutUrl, BigDecimal amount) {
        return create(eventId, serviceOrderId, paymentId, NotificationType.PAYMENT_CHECKOUT_CREATED,
                NotificationMessage.forPaymentCheckout(serviceOrderId, amount), validatedCheckoutUrl(checkoutUrl));
    }

    @Override
    public boolean confirmed(UUID eventId, UUID serviceOrderId, UUID paymentId, BigDecimal amount) {
        return create(eventId, serviceOrderId, paymentId, NotificationType.PAYMENT_CONFIRMED,
                NotificationMessage.forPaymentConfirmed(serviceOrderId, amount), null);
    }

    @Override
    public boolean failed(UUID eventId, UUID serviceOrderId, UUID paymentId, String reason) {
        return create(eventId, serviceOrderId, paymentId, NotificationType.PAYMENT_FAILED,
                NotificationMessage.forPaymentFailed(serviceOrderId, reason), null);
    }

    private boolean create(UUID eventId, UUID serviceOrderId, UUID paymentId, NotificationType type,
                           NotificationMessage message, String checkoutUrl) {
        UUID customerId = customerLookup.findCustomerId(serviceOrderId);
        return repository.createIfAbsent(Notification.createForPayment(eventId, type, paymentId,
                serviceOrderId, customerId, message, checkoutUrl));
    }

    private static String validatedCheckoutUrl(String checkoutUrl) {
        URI url = URI.create(checkoutUrl);
        if (!"https".equalsIgnoreCase(url.getScheme()) || url.getHost() == null
                || url.getUserInfo() != null || (url.getPort() != -1 && url.getPort() != 443)) {
            throw new IllegalArgumentException("Invalid Mercado Pago checkout URL");
        }
        String host = url.getHost().toLowerCase(Locale.ROOT);
        if (!host.equals("mercadopago.com.br") && !host.endsWith(".mercadopago.com.br")
                && !host.equals("mercadopago.com") && !host.endsWith(".mercadopago.com")) {
            throw new IllegalArgumentException("Invalid Mercado Pago checkout host");
        }
        return checkoutUrl;
    }
}
