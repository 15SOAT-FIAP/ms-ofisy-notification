package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.notifications.adapters.listeners.dto.PaymentFailedPayload;
import br.com.ofisy.notifications.application.notification.createpayment.CreatePaymentNotificationUseCase;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

@Component
class PaymentFailedEventHandler implements NotificationEventHandler {
    private final CreatePaymentNotificationUseCase useCase;
    private final JsonMapper jsonMapper;

    PaymentFailedEventHandler(CreatePaymentNotificationUseCase useCase, JsonMapper jsonMapper) {
        this.useCase = useCase;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String eventType() {
        return "PaymentFailed";
    }

    @Override
    public Result handle(EventEnvelope event) {
        var orderId = Objects.requireNonNull(event.serviceOrderId(), "serviceOrderId is required for PaymentFailed");
        var payload = jsonMapper.treeToValue(event.payload(), PaymentFailedPayload.class);
        boolean created = useCase.failed(event.eventId(), orderId, payload.paymentId(), payload.reason());
        return new Result(payload.paymentId(), created);
    }
}
