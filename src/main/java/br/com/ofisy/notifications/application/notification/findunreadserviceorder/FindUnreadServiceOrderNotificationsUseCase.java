package br.com.ofisy.notifications.application.notification.findunreadserviceorder;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;

import java.util.UUID;

public interface FindUnreadServiceOrderNotificationsUseCase {
    PaginatedResult<Notification> execute(UUID customerId, int limit, String cursor);
}
