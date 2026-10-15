package br.com.ofisy.notifications.application.notification.findallserviceorder;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;

public interface FindAllServiceOrderNotificationsUseCase {
    PaginatedResult<Notification> execute(int limit, String cursor);
}
