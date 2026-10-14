package br.com.ofisy.notifications.application.notification.findunreadserviceorder;

import br.com.ofisy.notifications.domain.notification.Notification;
import br.com.ofisy.notifications.domain.notification.PaginatedResult;
import java.util.Map;
import br.com.ofisy.notifications.domain.notification.NotificationRepository;
import br.com.ofisy.notifications.domain.notification.NotificationType;
import org.springframework.stereotype.Service;

@Service
public class FindUnreadServiceOrderNotificationsService implements FindUnreadServiceOrderNotificationsUseCase {
    private final NotificationRepository repository;

    public FindUnreadServiceOrderNotificationsService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Override
    public PaginatedResult<Notification> execute(int limit, Map<String, String> lastEvaluatedKey) {
        return repository.findUnreadByType(NotificationType.QUOTE_GENERATED, limit, lastEvaluatedKey);
    }
}
