package br.com.ofisy.notifications.application.notification.createquote;

import br.com.ofisy.notifications.domain.notification.Notification;
import java.math.BigDecimal;
import java.util.UUID;

public interface CreateQuoteNotificationUseCase {

    Notification execute(CreateQuoteCommand command);

    record CreateQuoteCommand(
            UUID quoteId,
            UUID serviceOrderId,
            BigDecimal totalPrice
    ) {}
}
