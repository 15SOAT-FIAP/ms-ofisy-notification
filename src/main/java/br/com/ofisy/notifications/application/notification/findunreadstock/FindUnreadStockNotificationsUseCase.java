package br.com.ofisy.notifications.application.notification.findunreadstock;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface FindUnreadStockNotificationsUseCase {
    PaginatedResult<Notification> execute(int limit, Map<String, String> lastEvaluatedKey);
}
