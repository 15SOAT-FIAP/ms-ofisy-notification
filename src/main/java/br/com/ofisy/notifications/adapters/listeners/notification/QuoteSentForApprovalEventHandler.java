package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.notifications.adapters.listeners.dto.QuoteSentForApprovalPayload;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase.CreateQuoteCommand;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class QuoteSentForApprovalEventHandler implements NotificationEventHandler {
    private final CreateQuoteNotificationUseCase useCase;
    private final JsonMapper jsonMapper;

    QuoteSentForApprovalEventHandler(CreateQuoteNotificationUseCase useCase, JsonMapper jsonMapper) {
        this.useCase = useCase;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String eventType() {
        return "QuoteSentForApproval";
    }

    @Override
    public Result handle(EventEnvelope event) {
        if (event.serviceOrderId() == null) {
            throw new IllegalArgumentException("serviceOrderId is required for QuoteSentForApproval");
        }
        var payload = jsonMapper.treeToValue(event.payload(), QuoteSentForApprovalPayload.class);
        boolean created = useCase.executeEvent(event.eventId(), new CreateQuoteCommand(
                payload.quoteId(), event.serviceOrderId(), payload.totalPrice()));
        return new Result(payload.quoteId(), created);
    }
}
