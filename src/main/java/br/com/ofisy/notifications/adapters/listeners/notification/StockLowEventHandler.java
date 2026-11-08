package br.com.ofisy.notifications.adapters.listeners.notification;

import br.com.ofisy.notifications.adapters.listeners.dto.EventEnvelope;
import br.com.ofisy.notifications.adapters.listeners.dto.LowStockPayload;
import br.com.ofisy.notifications.application.notification.createlowstock.CreateLowStockNotificationUseCase;
import br.com.ofisy.notifications.application.notification.createlowstock.CreateLowStockNotificationUseCase.CreateLowStockCommand;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
class StockLowEventHandler implements NotificationEventHandler {
    private final CreateLowStockNotificationUseCase useCase;
    private final JsonMapper jsonMapper;

    StockLowEventHandler(CreateLowStockNotificationUseCase useCase, JsonMapper jsonMapper) {
        this.useCase = useCase;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public String eventType() {
        return "StockLow";
    }

    @Override
    public Result handle(EventEnvelope event) {
        var payload = jsonMapper.treeToValue(event.payload(), LowStockPayload.class);
        boolean created = useCase.executeEvent(event.eventId(), new CreateLowStockCommand(
                payload.stockId(), payload.productName(), payload.currentQuantity(), payload.minThreshold()));
        return new Result(payload.stockId(), created);
    }
}
