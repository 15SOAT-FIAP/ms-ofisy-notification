package br.com.ofisy.notifications.application.notification.createpayment;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreatePaymentNotificationUseCase {
    boolean checkoutCreated(UUID eventId, UUID serviceOrderId, UUID paymentId, String checkoutUrl, BigDecimal amount);
    boolean confirmed(UUID eventId, UUID serviceOrderId, UUID paymentId, BigDecimal amount);
    boolean failed(UUID eventId, UUID serviceOrderId, UUID paymentId, String reason);
}
