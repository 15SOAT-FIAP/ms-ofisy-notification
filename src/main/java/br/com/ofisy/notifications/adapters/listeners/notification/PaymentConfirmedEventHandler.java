package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.notifications.adapters.listeners.dto.PaymentConfirmedPayload;
import br.com.ofisy.notifications.application.notification.createpayment.CreatePaymentNotificationUseCase;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

@Component
class PaymentConfirmedEventHandler implements NotificationEventHandler {
    private final CreatePaymentNotificationUseCase useCase;
    private final JsonMapper jsonMapper;

    PaymentConfirmedEventHandler(CreatePaymentNotificationUseCase useCase, JsonMapper jsonMapper) {
        this.useCase = useCase;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String eventType() {
        return "PaymentConfirmed";
    }

    @Override
    public Result handle(EventEnvelope event) {
        var orderId = Objects.requireNonNull(event.serviceOrderId(), "serviceOrderId is required for PaymentConfirmed");
        var payload = jsonMapper.treeToValue(event.payload(), PaymentConfirmedPayload.class);
        boolean created = useCase.confirmed(event.eventId(), orderId, payload.paymentId(), payload.amount());
        return new Result(payload.paymentId(), created);
    }
}
