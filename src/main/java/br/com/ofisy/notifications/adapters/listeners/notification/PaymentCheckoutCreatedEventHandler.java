package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.notifications.adapters.listeners.dto.PaymentCheckoutCreatedPayload;
import br.com.ofisy.notifications.application.notification.createpayment.CreatePaymentNotificationUseCase;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

@Component
class PaymentCheckoutCreatedEventHandler implements NotificationEventHandler {
    private final CreatePaymentNotificationUseCase useCase;
    private final JsonMapper jsonMapper;

    PaymentCheckoutCreatedEventHandler(CreatePaymentNotificationUseCase useCase, JsonMapper jsonMapper) {
        this.useCase = useCase;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String eventType() {
        return "PaymentCheckoutCreated";
    }

    @Override
    public Result handle(EventEnvelope event) {
        var orderId = Objects.requireNonNull(event.serviceOrderId(), "serviceOrderId is required for PaymentCheckoutCreated");
        var payload = jsonMapper.treeToValue(event.payload(), PaymentCheckoutCreatedPayload.class);
        boolean created = useCase.checkoutCreated(event.eventId(), orderId, payload.paymentId(), payload.checkoutUrl(), payload.amount());
        return new Result(payload.paymentId(), created);
    }
}
