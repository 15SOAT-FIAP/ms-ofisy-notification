package br.com.ofisy.notifications.application.notification.findunreadserviceorder;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;

public interface FindUnreadServiceOrderNotificationsUseCase {
    PaginatedResult<Notification> execute(int limit, String cursor);
}
