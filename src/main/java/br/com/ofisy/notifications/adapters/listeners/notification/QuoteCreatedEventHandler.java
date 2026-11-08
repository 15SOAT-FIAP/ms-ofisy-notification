package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.notifications.adapters.listeners.dto.QuoteGeneratedPayload;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase;
import br.com.ofisy.notifications.application.notification.createquote.CreateQuoteNotificationUseCase.CreateQuoteCommand;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class QuoteCreatedEventHandler implements NotificationEventHandler {
    private final CreateQuoteNotificationUseCase useCase;
    private final JsonMapper jsonMapper;

    QuoteCreatedEventHandler(CreateQuoteNotificationUseCase useCase, JsonMapper jsonMapper) {
        this.useCase = useCase;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String eventType() {
        return "QuoteCreated";
    }

    @Override
    public Result handle(EventEnvelope event) {
        if (event.serviceOrderId() == null) {
            throw new IllegalArgumentException("serviceOrderId is required for QuoteCreated");
        }
        var payload = jsonMapper.treeToValue(event.payload(), QuoteGeneratedPayload.class);
        boolean created = useCase.executeEvent(event.eventId(), new CreateQuoteCommand(
                payload.quoteId(), event.serviceOrderId(), payload.totalPrice()));
        return new Result(payload.quoteId(), created);
    }
}
