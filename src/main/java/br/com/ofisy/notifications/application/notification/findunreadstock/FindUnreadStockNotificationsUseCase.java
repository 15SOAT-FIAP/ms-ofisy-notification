package br.com.ofisy.notifications.application.notification.findunreadstock;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;

public interface FindUnreadStockNotificationsUseCase {
    PaginatedResult<Notification> execute(int limit, String cursor);
}
