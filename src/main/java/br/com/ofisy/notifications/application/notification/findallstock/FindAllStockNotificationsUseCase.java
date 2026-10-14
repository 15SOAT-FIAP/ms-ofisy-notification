package br.com.ofisy.notifications.application.notification.findallstock;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FindAllStockNotificationsUseCase {
    PaginatedResult<Notification> execute(int limit, Map<String, String> lastEvaluatedKey);
}
