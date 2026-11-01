package br.com.ofisy.notifications.application.notification.createquote;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreateQuoteNotificationUseCase {

    /** Quote creation requires an event identity for atomic deduplication. */
    boolean executeEvent(UUID eventId, CreateQuoteCommand command);

    record CreateQuoteCommand(
            UUID quoteId,
            UUID serviceOrderId,
            BigDecimal totalPrice
    ) {}
}
