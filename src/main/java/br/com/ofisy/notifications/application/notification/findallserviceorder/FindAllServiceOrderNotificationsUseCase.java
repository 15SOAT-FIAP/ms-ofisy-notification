package br.com.ofisy.notifications.application.notification.findallserviceorder;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;

public interface FindAllServiceOrderNotificationsUseCase {
    PaginatedResult<Notification> execute(int limit, Map<String, String> lastEvaluatedKey);
}
